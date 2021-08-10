package io.github.noeppi_noeppi.tools.modgradle.plugins.cursedep;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

// Load mods and modpack dependencies from CurseForge using CurseMaven
public class CurseDepPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        project.getRepositories().maven(r -> {
            r.setUrl("https://www.cursemaven.com/");
            r.content(c -> c.includeGroup("curse.maven"));
        });
        project.getExtensions().create(CurseDependencyExtension.EXTENSION_NAME, CurseDependencyExtension.class, project);
    }
}
