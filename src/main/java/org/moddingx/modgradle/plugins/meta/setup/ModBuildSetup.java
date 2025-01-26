package org.moddingx.modgradle.plugins.meta.setup;

import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Provider;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.moddingx.launcherlib.util.Artifact;
import org.moddingx.modgradle.plugins.meta.delegate.ModConfig;
import org.moddingx.modgradle.util.JavaGradlePluginUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModBuildSetup {
    
    public static void configureBuild(ProjectContext ctx, ModConfig config) throws IOException {
        ModContext mod = new ModContext(ctx, config);

        if (config.git.url != null) ctx.modProperty("source_url", config.git.url);
        if (config.git.issues != null) ctx.modProperty("issue_url", config.git.issues);

        mod.project().getPlugins().apply("java-library");
        mod.project().getPlugins().apply("net.neoforged.gradle.userdev");

        JavaGradlePluginUtils.getJavaExtension(ctx.project()).get().getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(mod.java()));

        System.out.println("Java: " + System.getProperty("java.version") + "   JVM: " + System.getProperty("java.vm.version") + "(" + System.getProperty("java.vendor") + ")   Arch: " + System.getProperty("os.arch"));
        System.out.println("Mod: " + ctx.project().getName() + " (" + mod.modid() + ")   Group: " + mod.group() + "   Version: " + mod.version());
        System.out.println("Minecraft: " + mod.minecraft() + "   NeoForge: " + mod.neoforge() + "   Target: java" + mod.java());
        System.out.println();

        Provider<Configuration> localRuntime = switch (ctx.project().getConfigurations().findByName("localRuntime")) {
            case null -> ctx.project().getConfigurations().register("localRuntime");
            case Configuration c -> ctx.project().provider(() -> c);
        };
        ctx.project().getConfigurations().named("runtimeClasspath").configure(runtimeClasspath -> runtimeClasspath.extendsFrom(localRuntime.get()));
        ctx.project().getDependencies().add("implementation", Artifact.from("net.neoforged", "neoforge", mod.neoforge()).getDescriptor());

        Minecraft minecraft = ctx.project().getExtensions().getByType(Minecraft.class);
        for (File resourceDir : JavaGradlePluginUtils.getJavaResourceDirs(ctx.project()).get().getFiles()) {
            Path accessTransformer = resourceDir.toPath().resolve("META-INF/accesstransformer.cfg");
            if (Files.isRegularFile(accessTransformer)) {
                minecraft.getAccessTransformers().file(accessTransformer.toFile());
            }
        }
        
        ModMappingsSetup.configureBuild(mod, config.mappings);
        ModRunsSetup.configureBuild(mod, config.runs);
        ModResourcesSetup.configureBuild(mod, config.resources);
        ModIntegrationSetup.configureBuild(mod);
        ModArtifactSetup.ConfiguredArtifacts artifacts = ModArtifactSetup.configureBuild(mod, config.artifacts);
        ModPublishSetup.configureBuild(mod, artifacts, config.publishing, config.git);
        ModUploadSetup.configureBuild(mod, artifacts, config.upload);
    }
}
