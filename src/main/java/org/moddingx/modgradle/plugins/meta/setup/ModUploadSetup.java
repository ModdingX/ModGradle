package org.moddingx.modgradle.plugins.meta.setup;

import com.modrinth.minotaur.ModrinthExtension;
import groovy.lang.Closure;
import net.darkhax.curseforgegradle.TaskPublishCurseForge;
import net.darkhax.curseforgegradle.UploadArtifact;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;
import org.moddingx.modgradle.plugins.meta.delegate.ModUploadsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModUploadSetup {

    public static void configureBuild(ModContext mod, ModArtifactSetup.ConfiguredArtifacts artifacts, ModUploadsConfig config) {
        TaskProvider<Task> uploadTask = mod.project().getTasks().register("upload", task -> task.setGroup("publishing"));
        if (config.curseforge.projectId != 0) {
            mod.project().getPlugins().apply("net.darkhax.curseforgegradle");
            String changelog = mod.changelog();
            TaskProvider<TaskPublishCurseForge> curseForgeTask = mod.project().getTasks().register("uploadCurseforge", TaskPublishCurseForge.class, task -> {
                task.setGroup("publishing");
                String secret = System.getenv(config.curseforge.secretEnv);
                if (secret != null && !secret.isBlank()) {
                    task.apiToken = secret;
                }

                if (!config.curseforge.inferDefaultVersions) task.disableVersionDetection();
                UploadArtifact mainFile = task.upload(config.curseforge.projectId, artifacts.jar());
                List<UploadArtifact> files = new ArrayList<>();
                files.add(mainFile);
                if (artifacts.sources() != null && artifacts.sources().upload()) {
                    files.add(mainFile.withAdditionalFile(artifacts.sources().task()));
                }
                if (artifacts.javadoc() != null && artifacts.javadoc().upload()) {
                    files.add(mainFile.withAdditionalFile(artifacts.javadoc().task()));
                }

                if (config.curseforge.inferDefaultVersions) {
                    mainFile.addModLoader("NeoForge");
                    mainFile.addJavaVersion("Java" + mod.java());
                    if (config.curseforge.versions.isEmpty()) {
                        mainFile.addGameVersion(mod.minecraft());
                    }
                }
                for (String version : config.curseforge.versions) {
                    mainFile.addGameVersion(version);
                }

                for (UploadArtifact file : files) {
                    file.releaseType = config.curseforge.type.name().toLowerCase(Locale.ROOT);
                    file.changelog = changelog;
                    file.changelogType = "markdown";
                    for (String dependency : config.curseforge.requirements) file.addRequirement(dependency);
                    for (String dependency : config.curseforge.optionals) file.addOptional(dependency);
                }

                for (Closure<?> closure : config.curseforge.cfgradle) {
                    task.configure(closure);
                }
            });
            uploadTask.configure(task -> task.dependsOn(curseForgeTask));
        }
        if (config.modrinth.projectId != null) {
            mod.project().getPlugins().apply("com.modrinth.minotaur");
            String changelog = mod.changelog();
            ModrinthExtension ext = mod.project().getExtensions().getByType(ModrinthExtension.class);

            String secret = System.getenv(config.modrinth.secretEnv);
            if (secret != null && !secret.isBlank()) {
                ext.getToken().set(secret);
            }

            ext.getProjectId().set(config.modrinth.projectId);
            ext.getVersionNumber().set(mod.version());
            ext.getVersionName().set(artifacts.jar().get().getArchiveFileName());
            ext.getUploadFile().set(artifacts.jar());
            if (artifacts.sources() != null && artifacts.sources().upload()) {
                ext.getAdditionalFiles().add(artifacts.sources().task());
            }
            if (artifacts.javadoc() != null && artifacts.javadoc().upload()) {
                ext.getAdditionalFiles().add(artifacts.javadoc().task());
            }
            ext.getChangelog().set(changelog);
            ext.getVersionType().set(config.modrinth.type.name().toLowerCase(Locale.ROOT));
            ext.getDetectLoaders().set(config.modrinth.inferDefaultVersions);
            if (config.modrinth.inferDefaultVersions) {
                ext.getLoaders().add("neoforge");
                if (config.modrinth.versions.isEmpty()) ext.getGameVersions().add(mod.minecraft());
            }
            for (String version : config.modrinth.versions) {
                ext.getGameVersions().add(version);
            }

            for (String dependency : config.modrinth.requirements) ext.getRequired().project(dependency);
            for (String dependency : config.modrinth.optionals) ext.getOptional().project(dependency);

            for (Closure<?> closure : config.modrinth.minotaur) {
                closure.setDelegate(ext);
                closure.setResolveStrategy(Closure.DELEGATE_FIRST);
                closure.call();
            }

            mod.project().afterEvaluate(p -> uploadTask.configure(task -> task.dependsOn(p.getTasks().named("modrinth"))));
        }
    }
}
