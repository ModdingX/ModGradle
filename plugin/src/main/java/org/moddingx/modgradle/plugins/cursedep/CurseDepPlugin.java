package org.moddingx.modgradle.plugins.cursedep;

import net.minecraftforge.gradle.userdev.DependencyManagementExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.util.curse.CurseUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class CurseDepPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        ModGradle.initialiseProject(project);
        project.getRepositories().maven(r -> {
            r.setUrl(CurseUtil.CURSE_MAVEN);
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
            return CurseUtil.API.getSlug(projectId);
        } catch (IOException e) {
            return "unknown";
        }
    }
}