package org.moddingx.modgradle.plugins.meta.setup;

import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;
import org.gradle.plugins.ide.idea.model.IdeaModel;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ModIntegrationSetup {

    public static void configureBuild(ModContext mod) {
        // Annotation processor build properties
        mod.project().getTasks().named("compileJava", JavaCompile.class).configure(jc -> {
            jc.getOptions().getCompilerArgs().addAll(List.of(
                    "-Amod.properties.mod_id=" + mod.modid(),
                    "-Amod.properties.mc_version=" + mod.minecraft(),
                    "-Amod.properties.mod_version=" + mod.version(),
                    "-Amod.properties.java_version=" + mod.java()
            ));
        });

        // Jar manifest
        mod.project().getTasks().named("jar", Jar.class).configure(jar -> {
            jar.getManifest().getAttributes().putAll(Map.of(
                    "Specification-Title", mod.modid(),
                    "Specification-Version", "1",
                    "Implementation-Title", mod.project().getName(),
                    "Implementation-Version", mod.version()
            ));
            if (mod.timestamp() != null) {
                jar.getManifest().getAttributes().put("Implementation-Timestamp", mod.timestamp());
            }
            jar.getManifest().getAttributes().put("Automatic-Module-Name", "mcmods." + mod.modid());
        });

        // Explicitly set charset and make reproducible jars
        mod.project().afterEvaluate(p -> {
            p.getTasks().withType(JavaCompile.class).configureEach(jc -> jc.getOptions().setEncoding(StandardCharsets.UTF_8.name()));
            p.getTasks().withType(Test.class).configureEach(test -> test.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name()));
            p.getTasks().withType(Javadoc.class).configureEach(jd -> jd.getOptions().setEncoding(StandardCharsets.UTF_8.name()));
            p.getTasks().withType(AbstractArchiveTask.class).configureEach(archive -> {
                archive.setPreserveFileTimestamps(false);
                archive.setReproducibleFileOrder(true);
            });
        });

        // Idea sources
        mod.project().getPlugins().apply("idea");
        if (mod.project().getExtensions().findByName("idea") instanceof IdeaModel idea) {
            idea.getModule().setDownloadSources(true);
            idea.getModule().setDownloadJavadoc(true);
        }
    }
}
