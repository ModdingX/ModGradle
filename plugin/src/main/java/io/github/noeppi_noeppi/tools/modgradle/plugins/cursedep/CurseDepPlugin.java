package io.github.noeppi_noeppi.tools.modgradle.plugins.cursedep;

import com.google.gson.JsonElement;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.userdev.DependencyManagementExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

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

    public static String curseArtifact(int projectId, int fileId) {
        return curseArtifact(getSlug(projectId), projectId, fileId);
    }
    
    public static String curseArtifact(String slug, int projectId, int fileId) {
        return "curse.maven:" + slug + "-" + projectId + ":" + fileId;
    }

    public static String getSlug(int projectId) {
        try {
            Reader reader = new InputStreamReader(new URL("https://addons-ecs.forgesvc.net/api/v2/addon/" + projectId).openStream());
            JsonElement json = ModGradle.INTERNAL.fromJson(reader, JsonElement.class);
            reader.close();
            return json.getAsJsonObject().get("slug").getAsString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
