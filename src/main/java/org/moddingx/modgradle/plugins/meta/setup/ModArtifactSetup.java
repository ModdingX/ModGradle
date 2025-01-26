package org.moddingx.modgradle.plugins.meta.setup;

import jakarta.annotation.Nullable;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.moddingx.modgradle.plugins.javadoc.JavadocConfigureTask;
import org.moddingx.modgradle.plugins.meta.delegate.ModArtifactsConfig;
import org.moddingx.modgradle.util.JavaGradlePluginUtils;

public class ModArtifactSetup {

    public static ConfiguredArtifacts configureBuild(ModContext mod, ModArtifactsConfig config) {
        BuildableArtifact sourceArtifact = null;
        BuildableArtifact javadocArtifact = null;
        TaskProvider<Jar> jar = mod.project().getTasks().named("jar", Jar.class);
        TaskProvider<Task> build = mod.project().getTasks().named("build");
        if (config.sources != null) {
            TaskProvider<Jar> sourceJarTask = mod.project().getTasks().register("sourcesJar", Jar.class, task -> {
                task.setGroup("build");
                task.getArchiveClassifier().set("sources");
                task.from(JavaGradlePluginUtils.getJavaSourceDirs(mod.project()));
                task.from(JavaGradlePluginUtils.getJavaResourceDirs(mod.project()));
                for (SourceSet sourceSet : mod.additionalModSources()) {
                    task.from(sourceSet.getJava().getSrcDirs());
                    task.from(sourceSet.getResources().getSrcDirs());
                }
            });
            mod.project().afterEvaluate(p -> sourceJarTask.configure(task -> task.setManifest(jar.get().getManifest())));
            build.configure(task -> task.dependsOn(sourceJarTask));
            sourceArtifact = new BuildableArtifact(sourceJarTask, config.sources.publishToRepositories, config.sources.uploadToModHostingSites);
        }
        if (config.javadoc != null) {
            TaskProvider<Javadoc> javadocTask = mod.project().getTasks().named("javadoc", Javadoc.class);
            TaskProvider<Jar> javadocJarTask = mod.project().getTasks().register("javadocJar", Jar.class, task -> {
                task.setGroup("build");
                task.getArchiveClassifier().set("javadoc");
                task.dependsOn(javadocTask);
                task.from(javadocTask.get().getDestinationDir());
            });
            mod.project().afterEvaluate(p -> {
                @Nullable Task javadocMetaTask = p.getTasks().findByName("javadocMeta");
                if (javadocMetaTask != null) javadocJarTask.configure(task -> task.dependsOn(javadocMetaTask));
                javadocJarTask.configure(task -> task.setManifest(jar.get().getManifest()));
            });
            build.configure(task -> task.dependsOn(javadocJarTask));
            javadocArtifact = new BuildableArtifact(javadocJarTask, config.javadoc.publishToRepositories, config.javadoc.uploadToModHostingSites);
        }
        mod.project().afterEvaluate(p -> {
            if (p.getTasks().findByName("javadocConfigure") instanceof JavadocConfigureTask javadocConfigureTask) {
                for (SourceSet sourceSet : mod.additionalModSources()) {
                    javadocConfigureTask.from(sourceSet.getJava().getSourceDirectories());
                }
            }
        });
        return new ConfiguredArtifacts(jar, sourceArtifact, javadocArtifact);
    }

    public record ConfiguredArtifacts(TaskProvider<Jar> jar, @Nullable BuildableArtifact sources, @Nullable BuildableArtifact javadoc) {}
    public record BuildableArtifact(TaskProvider<? extends AbstractArchiveTask> task, boolean publish, boolean upload) {}
}
