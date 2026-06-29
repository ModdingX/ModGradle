package org.moddingx.modgradle.plugins.meta;

import jakarta.annotation.Nonnull;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.process.ExecOperations;
import org.moddingx.modgradle.ModGradle;

import javax.inject.Inject;

public abstract class MetaPlugin implements Plugin<Project> {

    @Inject
    public abstract ExecOperations getExecOperations();

    @Override
    public void apply(@Nonnull Project project) {
        ModGradle.init(project);
        this.setupRepositories(project);
        // Apply userdev early (plugin phase); applying it late from mod.configure breaks NeoGradle's
        // repository setup on a cold cache ("Cannot mutate content repository descriptor 'MavenRepo'").
        project.getPlugins().apply("java-library");
        project.getPlugins().apply("net.neoforged.gradle.userdev");
        project.getExtensions().create("mod", ModExtension.class, project, this.getExecOperations());
    }
    
    private void setupRepositories(Project project) {
        project.getRepositories().mavenCentral();
        project.getRepositories().maven(r -> {
            r.setName("Minecraft maven");
            r.setUrl("https://libraries.minecraft.net");
        });
        project.getRepositories().maven(r -> {
            r.setName("NeoForged maven");
            r.setUrl("https://maven.neoforged.net/releases");
        });
        project.getRepositories().maven(r -> {
            r.setName("ModdingX maven");
            r.setUrl("https://maven.moddingx.org/release");
        });
    }
}
