package org.moddingx.modgradle.plugins.meta.setup;

import groovy.lang.Closure;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.internal.ConfigureUtil;
import org.moddingx.modgradle.plugins.meta.delegate.ModRunsConfig;
import org.moddingx.modgradle.util.JavaGradlePluginUtils;

import java.util.List;

public class ModRunsSetup {

    public static void configureBuild(ModContext mod, ModRunsConfig config) {
        RunManager runs = mod.project().getExtensions().getByType(RunManager.class);
        configure(mod, runs.maybeCreate("client"), config.autoConfig, config.client);
        configure(mod, runs.maybeCreate("server"), config.autoConfig, config.server);
        if (mod.data() >= 61) { // 1.21.4
            configure(mod, runs.maybeCreate("clientData"), config.autoConfig, config.clientData);
            configure(mod, runs.maybeCreate("serverData"), config.autoConfig, config.serverData);
        } else {
            configure(mod, runs.maybeCreate("data"), config.autoConfig, config.clientData);
        }
        configure(mod, runs.maybeCreate("gameTestServer"), config.autoConfig, config.gameTestServer);
        JavaGradlePluginUtils.getJavaSources(mod.project()).get().resources(dirs -> dirs.srcDir(mod.project().file("src/generated/resources")));
    }

    private static void configure(ModContext mod, Run run, boolean autoConfig, List<Closure<?>> actions) {
        if (autoConfig) {
            run.workingDirectory(mod.project().file("runs").toPath().resolve(run.getName()).toFile());
            run.systemProperty("forge.logging.console.level", "debug");
            run.systemProperty("forge.logging.markers", "REGISTRIES");
            run.systemProperty("forge.enabledGameTestNamespaces", mod.modid());
            run.systemProperty("mixin.debug.export", "true");
            for (SourceSet sourceSet : mod.additionalModSources()) {
                run.modSource(sourceSet);
            }
            if ("server".equals(run.getName())) {
                run.getArguments().add("--nogui");
            }
            if ("data".equals(run.getName()) || "clientData".equals(run.getName()) || "serverData".equals(run.getName())) {
                run.getArguments().addAll(
                        "--mod", mod.modid(), "--all",
                        "--output", mod.project().file("src/generated/resources").getAbsolutePath(),
                        "--existing", mod.project().file("src/main/resources").getAbsolutePath()
                );
            }
        }
        for (Closure<?> action : actions) {
            ConfigureUtil.configureSelf(action, run);
        }
    }
}
