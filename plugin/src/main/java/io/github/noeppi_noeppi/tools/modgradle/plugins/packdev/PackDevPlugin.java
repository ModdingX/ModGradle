package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev;

import com.google.gson.JsonElement;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.api.Versioning;
import io.github.noeppi_noeppi.tools.modgradle.plugins.cursedep.CurseDepPlugin;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task.BuildCursePackTask;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task.BuildModrinthPackTask;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task.BuildServerPackTask;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task.BuildTargetTask;
import io.github.noeppi_noeppi.tools.modgradle.util.CurseUtil;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.MgUtil;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;
import net.minecraftforge.gradle.userdev.UserDevExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class PackDevPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        if (!project.getPlugins().hasPlugin("net.minecraftforge.gradle"))
            throw new IllegalStateException("The PackDev plugin requires the ForgeGradle userdev plugin.");
        if (!project.getPlugins().hasPlugin("io.github.noeppi_noeppi.tools.modgradle.mapping"))
            throw new IllegalStateException("The PackDev plugin requires the ModGradle mapping plugin.");

        project.getRepositories().maven(r -> {
            r.setUrl(CurseUtil.CURSE_MAVEN);
            r.content(c -> c.includeGroup("curse.maven"));
        });

        try {
            for (Side side : Side.values()) {
                if (!Files.exists(project.file("data").toPath().resolve(side.id))) {
                    Files.createDirectories(project.file("data").toPath().resolve(side.id));
                }
            }

            if (!Files.exists(project.file("modlist.json").toPath())) {
                Writer writer = Files.newBufferedWriter(project.file("modlist.json").toPath(), StandardOpenOption.CREATE_NEW);
                writer.write("[]\n");
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Configuration clientMods = project.getConfigurations().create("clientMods", c -> {
            c.setCanBeConsumed(false);
            c.setCanBeResolved(true);
        });
        Configuration serverMods = project.getConfigurations().create("serverMods", c -> {
            c.setCanBeConsumed(false);
            c.setCanBeResolved(true);
        });
        Configuration compileOnly = project.getConfigurations().getByName("compileOnly");
        compileOnly.extendsFrom(clientMods, serverMods);

        SourceSetContainer sourceSets = JavaEnv.getJavaExtension(project).get().getSourceSets();
        sourceSets.create("data", set -> {
            set.getJava().setSrcDirs(List.of());
            set.getResources().setSrcDirs(List.of(
                    project.file("data/" + Side.COMMON.id),
                    project.file("data/" + Side.CLIENT.id),
                    project.file("data/" + Side.SERVER.id)
            ));
        });

        SourceSet clientDepSources = sourceSets.create("modpack_dependency_client", set -> {
            set.getJava().setSrcDirs(List.of());
            set.getResources().setSrcDirs(List.of());
            set.setRuntimeClasspath(clientMods);
        });

        SourceSet serverDepSources = sourceSets.create("modpack_dependency_server", set -> {
            set.getJava().setSrcDirs(List.of());
            set.getResources().setSrcDirs(List.of());
            set.setRuntimeClasspath(serverMods);
        });

        UserDevExtension mcExt = getMcExt(project);
        mcExt.mappings("none", "none");
        addRunConfig(project, mcExt, "client", Side.CLIENT, JavaEnv.getJavaSources(project).get(), clientDepSources);
        addRunConfig(project, mcExt, "server", Side.SERVER, JavaEnv.getJavaSources(project).get(), serverDepSources);

        PackDevExtension ext = project.getExtensions().create(PackDevExtension.EXTENSION_NAME, PackDevExtension.class);
        project.afterEvaluate(p -> {
            PackSettings settings = ext.getSettings();
            JavaEnv.getJavaExtension(project).get().getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(Versioning.getJavaVersion(settings.minecraft())));

            try {
                for (Path path : Arrays.stream(Side.values()).map(side -> project.file("data/" + side.id).toPath()).filter(Files::notExists).toList()) {
                    Files.createDirectories(path);
                }
                for (String edition : settings.editions()) {
                    for (Path path : Arrays.stream(Side.values()).map(side -> project.file("data-" + edition + "/" + side.id).toPath()).filter(Files::notExists).toList()) {
                        Files.createDirectories(path);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load modlist", e);
            }

            List<CurseFile> files;
            try {
                Reader reader = Files.newBufferedReader(project.file("modlist.json").toPath());
                files = CurseFile.parseFiles(ModGradle.GSON.fromJson(reader, JsonElement.class));
                reader.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load modlist", e);
            }

            for (CurseFile file : files) {
                String cfg = switch (file.side()) {
                    case COMMON -> "implementation";
                    case CLIENT -> "clientMods";
                    case SERVER -> "serverMods";
                };
                project.getDependencies().add(cfg, CurseDepPlugin.curseArtifact(file.projectId(), file.fileId()));
            }

            forEditions(settings, edition -> {
                addBuildTask(project, BuildCursePackTask.class, edition, "curse", settings, files);
                addBuildTask(project, BuildServerPackTask.class, edition, "server", settings, files);
                addBuildTask(project, BuildModrinthPackTask.class, edition, "modrinth", settings, files);
            });
        });
    }

    private static UserDevExtension getMcExt(Project project) {
        try {
            return project.getExtensions().getByType(UserDevExtension.class);
        } catch (Exception e) {
            throw new IllegalStateException("minecraft extension not found.");
        }
    }

    private static void forEditions(PackSettings settings, Consumer<String> action) {
        action.accept(null);
        settings.editions().forEach(action);
    }

    private static void addRunConfig(Project project, UserDevExtension ext, String name, Side side, SourceSet commonMods, SourceSet additionalMods) {
        String capitalized = name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1);
        String taskName = "run" + capitalized;
        File workingDir = project.file(taskName);
        ext.getRuns().create(name, run -> {
            run.workingDirectory(workingDir);
            run.property("forge.logging.console.level", "info");
            run.jvmArg("-Dproduction=true");
            run.getMods().create("packdev_dummy_mod", mod -> {
                mod.source(commonMods);
                mod.source(additionalMods);
            });
        });

        Copy copyTask = project.getTasks().create("copy" + capitalized + "Data", Copy.class);
        copyTask.setDestinationDir(workingDir);
        copyTask.from(project.fileTree("data/" + Side.COMMON.id));
        if (side != Side.COMMON) copyTask.from(project.fileTree("data/" + side.id));
        // Create some directories because Forge 1.17+ requires it
        JavaCompile jc = MgUtil.task(project, "compileJava", JavaCompile.class);
        if (jc == null) throw new IllegalStateException("Cannot set up PackDev run config: compileJava task not found");
        copyTask.doLast(t -> {
            try {
                Files.createDirectories(jc.getDestinationDirectory().getAsFile().get().toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        project.getGradle().projectsEvaluated(g -> {
            Task prepareRunsTask = project.getTasks().getByName("prepareRuns");
            prepareRunsTask.dependsOn(copyTask);
        });
    }

    private static void addBuildTask(Project project, Class<? extends BuildTargetTask> taskCls, String edition, String classifier, PackSettings settings, List<CurseFile> files) {
        String editionPrefix = edition == null ? "" : edition + "-";
        String capitalizedClassifier = classifier.substring(0, 1).toUpperCase(Locale.ROOT) + classifier.substring(1);
        String capitalizedEdition = edition == null || edition.isEmpty() ? "" : edition.substring(0, 1).toUpperCase(Locale.ROOT) + edition.substring(1);
        BuildTargetTask task = project.getTasks().create("build" + capitalizedEdition + capitalizedClassifier + "Pack", taskCls, settings, files, edition == null ? "" : edition);
        task.getArchiveClassifier().convention(editionPrefix + classifier);
        task.getDestinationDirectory().set(project.file("build").toPath().resolve("target").toFile());
        Task buildTask = MgUtil.task(project, "build", Task.class);
        if (buildTask != null) buildTask.dependsOn(task);
    }
}
