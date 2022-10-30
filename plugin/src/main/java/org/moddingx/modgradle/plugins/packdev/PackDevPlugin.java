package org.moddingx.modgradle.plugins.packdev;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraftforge.gradle.common.util.Artifact;
import net.minecraftforge.gradle.mcp.tasks.GenerateSRG;
import net.minecraftforge.gradle.userdev.DependencyManagementExtension;
import net.minecraftforge.gradle.userdev.UserDevExtension;
import org.gradle.api.*;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.build.event.BuildEventsListenerRegistry;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.api.Versioning;
import org.moddingx.modgradle.plugins.packdev.cache.PackDevCache;
import org.moddingx.modgradle.plugins.packdev.platform.ModFile;
import org.moddingx.modgradle.plugins.packdev.platform.ModdingPlatform;
import org.moddingx.modgradle.util.JavaEnv;
import org.moddingx.modgradle.util.MgUtil;
import org.moddingx.modgradle.util.Side;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public abstract class PackDevPlugin implements Plugin<Project> {

    @Inject
    @SuppressWarnings("UnstableApiUsage")
    public abstract BuildEventsListenerRegistry getEventRegistry();

    @Override
    public void apply(@Nonnull Project project) {
        ModGradle.initialiseProject(project);

        if (!project.getPlugins().hasPlugin("net.minecraftforge.gradle")) {
            throw new IllegalStateException("The PackDev plugin requires the ForgeGradle userdev plugin.");
        }

        try {
            for (Side side : Side.values()) {
                if (!Files.exists(project.file("data").toPath().resolve(side.id))) {
                    Files.createDirectories(project.file("data").toPath().resolve(side.id));
                }
            }

            if (!Files.exists(project.file("modlist.json").toPath())) {
                Writer writer = Files.newBufferedWriter(project.file("modlist.json").toPath(), StandardOpenOption.CREATE_NEW);
                writer.write("{}\n");
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ModdingPlatform<?> platform;
        String modListMcVersion;
        List<JsonElement> fileData;
        try (Reader in = Files.newBufferedReader(project.file("modlist.json").toPath())) {
            JsonObject json = ModGradle.GSON.fromJson(in, JsonObject.class);

            int api = Objects.requireNonNull(json.get("api"), "No modlist.json API version found.").getAsInt();
            if (api != 2) {
                throw new IllegalStateException("Unsupported modlist.json API: " + api + ". This version of PackDev requires api version 2.");
            }

            platform = PackDevRegistry.getPlatform(Objects.requireNonNull(json.get("platform"), "No modding platform set.").getAsString());

            String loader = Objects.requireNonNull(json.get("loader"), "No mod loader set.").getAsString();
            if (!loader.toLowerCase(Locale.ROOT).equals("forge")) throw new IllegalStateException("PackDev can only build forge modpacks, " + loader + " is not supported.");
            modListMcVersion = Objects.requireNonNull(json.get("minecraft"), "No minecraft version set.").getAsString();

            fileData = new ArrayList<>();
            if (json.has("installed")) {
                json.get("installed").getAsJsonArray().forEach(fileData::add);
            }
            if (json.has("dependencies")) {
                json.get("dependencies").getAsJsonArray().forEach(fileData::add);
            }
            fileData = List.copyOf(fileData);
        } catch (JsonSyntaxException e) {
            throw new IllegalStateException("Invalid modlist.json: " + e.getMessage(), e);
        } catch (NullPointerException e) {
            throw new IllegalStateException("Failed to read modlist.json: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        platform.initialise(project);
        PackDevCache cache = new PackDevCache(project, platform);
        //noinspection UnstableApiUsage
        this.getEventRegistry().onTaskCompletion(project.provider(() -> e -> cache.save()));
        List<? extends ModFile> files = List.copyOf(platform.readModList(project, cache, fileData));

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

        DependencyManagementExtension fgExt = getMcExt(project, "fg", DependencyManagementExtension.class);
        for (ModFile file : files) {
            String cfg = switch (file.fileSide()) {
                case COMMON -> "implementation";
                case CLIENT -> "clientMods";
                case SERVER -> "serverMods";
            };
            Artifact artifact = file.createDependency();
            ExternalModuleDependency dependency = (ExternalModuleDependency) project.getDependencies().create(artifact.getDescriptor());
            project.getDependencies().add(cfg, fgExt.deobf(dependency));
        }

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

        UserDevExtension mcExt = getMcExt(project, "minecraft", UserDevExtension.class);
        // Pass modListMcVersion as minecraft version here as pack settings are not yet available
        // modListMcVersion will always match the actual minecraft version or an exception will be thrown later in afterEvaluate
        addRunConfig(project, mcExt, "client", Side.CLIENT, JavaEnv.getJavaSources(project).get(), clientDepSources, modListMcVersion);
        addRunConfig(project, mcExt, "server", Side.SERVER, JavaEnv.getJavaSources(project).get(), serverDepSources, modListMcVersion);

        PackDevExtension ext = project.getExtensions().create(PackDevExtension.EXTENSION_NAME, PackDevExtension.class);

        project.afterEvaluate(p -> {
            PackSettings settings = ext.getSettings();
            if (!Objects.equals(settings.minecraft(), modListMcVersion)) {
                throw new IllegalStateException("Minecraft version from modlist does not match installed version: modlist=" + modListMcVersion + ", installed=" + settings.minecraft());
            }

            JavaEnv.getJavaExtension(project).get().getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(Versioning.getJavaVersion(settings.minecraft())));

            Map<String, Optional<Object>> targets = ext.getAllTargets();
            if (targets.isEmpty()) {
                System.err.println("Warning: No modpack targets defined.");
            } else {
                targets.entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .forEach(target -> addBuildTask(project, target.getKey(), platform, settings, files, target.getValue().orElse(null)));
            }
        });
    }

    private static <T> T getMcExt(Project project, String name, Class<T> cls) {
        try {
            return project.getExtensions().getByType(cls);
        } catch (Exception e) {
            throw new IllegalStateException(name + " extension not found.");
        }
    }

    private static void addRunConfig(Project project, UserDevExtension ext, String name, Side side, SourceSet commonMods, SourceSet additionalMods, String mcVersion) {
        String capitalized = MgUtil.capitalize(name);
        String taskName = "run" + capitalized;
        File workingDir = project.file(taskName);
        ext.getRuns().create(name, run -> {
            run.workingDirectory(workingDir);
            run.property("forge.logging.console.level", "info");
            if (Versioning.getMixinVersion(mcVersion) != null) {
                GenerateSRG generateMappings = MgUtil.task(project, "createMcpToSrg", GenerateSRG.class);
                if (generateMappings != null) {
                    run.property("mixin.env.remapRefMap", "true");
                    run.property("mixin.env.refMapRemappingFile", generateMappings.getOutput().get().getAsFile().toPath().toAbsolutePath().normalize().toString());
                }
            }
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
        Task createDirTask = project.getTasks().create("prepare" + capitalized + "Data", DefaultTask.class);
        //noinspection Convert2Lambda
        createDirTask.doLast(new Action<>() {
            @Override
            public void execute(@Nonnull Task task) {
                try {
                    Files.createDirectories(jc.getDestinationDirectory().getAsFile().get().toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        createDirTask.getOutputs().upToDateWhen(t -> false);

        project.getGradle().projectsEvaluated(g -> {
            Task prepareRunsTask = project.getTasks().getByName("prepareRuns");
            prepareRunsTask.dependsOn(copyTask);
            prepareRunsTask.dependsOn(createDirTask);
        });
    }

    private static void addBuildTask(Project project, String id, ModdingPlatform<?> platform, PackSettings settings, List<? extends ModFile> files, @Nullable Object properties) {
        Task task = PackDevRegistry.createTargetTask(project, id, platform, settings, files, properties);
        if (task instanceof AbstractArchiveTask archive) {
            archive.getDestinationDirectory().set(project.file("build").toPath().resolve("target").toFile());
            archive.getArchiveBaseName().convention(project.provider(project::getName));
            archive.getArchiveVersion().convention(project.provider(() -> project.getVersion().toString()));
            archive.getArchiveClassifier().convention(id.toLowerCase(Locale.ROOT));
        }
        Task buildTask = MgUtil.task(project, "build", Task.class);
        if (buildTask != null) buildTask.dependsOn(task);
    }
}
