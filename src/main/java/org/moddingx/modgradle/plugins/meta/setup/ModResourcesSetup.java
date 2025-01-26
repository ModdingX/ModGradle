package org.moddingx.modgradle.plugins.meta.setup;

import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.moddingx.modgradle.plugins.meta.delegate.ModResourcesConfig;

import java.util.Map;

public class ModResourcesSetup {

    public static void configureBuild(ModContext mod, ModResourcesConfig config) {
        TaskProvider<ProcessResources> task = mod.project().getTasks().named("processResources", ProcessResources.class);
        task.configure(res -> res.exclude("/.cache/**"));
        if (!config.expandingPatterns.isEmpty()) {
            task.configure(res -> {
                for (String pattern : config.expandingPatterns) {
                    res.filesMatching(pattern, details -> {
                        details.expand(Map.of("mod", mod.properties()));
                    });
                }
            });
            mod.dependsOnProperties(task);
        }
    }
}
