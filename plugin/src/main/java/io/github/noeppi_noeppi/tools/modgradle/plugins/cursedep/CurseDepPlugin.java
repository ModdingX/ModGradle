package io.github.noeppi_noeppi.tools.modgradle.plugins.cursedep;

import net.minecraftforge.gradle.userdev.DependencyManagementExtension;
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
        DependencyManagementExtension ext = getDepExt(project);
        if (ext == null) System.err.println("ModGradle cursedep can't find a DependencyManagementExtension. Curse dependencies can't be deobfuscated.");
        project.getExtensions().create(CurseDependencyExtension.EXTENSION_NAME, CurseDependencyExtension.class, project, ext);
    }
    
    private static DependencyManagementExtension getDepExt(Project project) {
        try {
            return project.getExtensions().getByType(DependencyManagementExtension.class);
        } catch (Exception e) {
            return null;
        }
    }
}
