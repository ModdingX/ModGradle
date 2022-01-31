package io.github.noeppi_noeppi.tools.modgradle.plugins.cursedep;

import io.github.noeppi_noeppi.tools.cursewrapper.api.CurseWrapper;
import net.minecraftforge.gradle.userdev.DependencyManagementExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;

public class CurseDepPlugin implements Plugin<Project> {

    private static final CurseWrapper API = new CurseWrapper(URI.create("https://curse.melanx.de/"));

    @Override
    public void apply(Project project) {
        project.getRepositories().maven(r -> {
            r.setUrl("https://cfa2.cursemaven.com");
            r.content(c -> c.includeGroup("curse.maven"));
        });
        DependencyManagementExtension ext = getDepExt(project);
        project.getExtensions().create(CurseDependencyExtension.EXTENSION_NAME, CurseDependencyExtension.class, project, ext);
    }

    private static DependencyManagementExtension getDepExt(Project project) {
        return project.getExtensions().getByType(DependencyManagementExtension.class);
    }

    public static String curseArtifact(int projectId, int fileId) {
        return curseArtifact(getSlug(projectId), projectId, fileId);
    }

    public static String curseArtifact(String slug, int projectId, int fileId) {
        return curseArtifact(slug, projectId, fileId, null);
    }

    public static String curseArtifact(String slug, int projectId, int fileId, @Nullable String extension) {
        return "curse.maven:" + slug + "-" + projectId + ":" + fileId + (extension == null ? "" : "@" + extension);
    }

    public static String getSlug(int projectId) {
        try {
            return API.getSlug(projectId);
        } catch (IOException e) {
            return "unknown";
        }
    }
}