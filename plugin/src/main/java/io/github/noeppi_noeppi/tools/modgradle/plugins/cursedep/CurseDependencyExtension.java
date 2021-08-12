package io.github.noeppi_noeppi.tools.modgradle.plugins.cursedep;

import com.google.gson.JsonElement;
import groovy.lang.GroovyObjectSupport;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.util.MavenArtifactDownloader;
import net.minecraftforge.gradle.userdev.DependencyManagementExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CurseDependencyExtension extends GroovyObjectSupport {

    public static final String EXTENSION_NAME = "curse";

    private final Project project;
    
    @Nullable
    private final DependencyManagementExtension ext;

    public CurseDependencyExtension(Project project, @Nullable DependencyManagementExtension ext) {
        this.project = project;
        this.ext = ext;
    }

    public Dependency mod(int projectId, int fileId) {
        return this.createDependency(curseArtifact(getSlug(projectId), projectId, fileId));
    }

    public Dependency pack(int projectId, int fileId) {
        return this.pack(projectId, fileId, Collections.emptyList());
    }
    
    public Dependency pack(int projectId, int fileId, List<Object> excludes) {
        Set<Integer> idExcludes = new HashSet<>();
        Set<String> slugExcludes = new HashSet<>();
        for (Object exclude : excludes) {
            if (exclude instanceof Integer) {
                idExcludes.add((Integer) exclude);
            } else if (exclude instanceof String) {
                slugExcludes.add((String) exclude);
            } else {
                throw new IllegalStateException("Can't add curse ModPack dependency: Excludes must be int (project id) or String (slug)");
            }
        }
        String configurationName = "cursepack_" + projectId + "_" + fileId + "_" + idExcludes.stream().sorted().map(Object::toString).collect(Collectors.joining("_")) + "_" + slugExcludes.stream().sorted().collect(Collectors.joining("_"));
        Configuration cache = this.project.getConfigurations().findByName(configurationName);
        if (cache != null) {
            return this.project.getDependencies().create(cache);
        } else {
            // Don't need a real slug here as it won't be visible anywhere
            // Saves us one call to the API
            File file = MavenArtifactDownloader.download(this.project, curseArtifact("cursepack", projectId, fileId), false);
            if (file == null) {
                throw new IllegalStateException("Can't create curse ModPack dependency: Failed to download manifest");
            } else try {
                ZipFile zipFile = new ZipFile(file);
                ZipEntry entry = zipFile.getEntry("manifest.json");
                if (entry == null) entry = zipFile.getEntry("/manifest.json");
                if (entry == null)
                    throw new IllegalStateException("Can't create curse ModPack dependency: Pack file contains no manifest.");
                Reader reader = new InputStreamReader(zipFile.getInputStream(entry));
                JsonElement json = ModGradle.GSON.fromJson(reader, JsonElement.class);
                reader.close();
                Configuration configuration = this.project.getConfigurations().create(configurationName);
                for (JsonElement fileJson : json.getAsJsonObject().get("files").getAsJsonArray()) {
                    int p = fileJson.getAsJsonObject().get("projectID").getAsInt();
                    if (!idExcludes.contains(p)) {
                        int f = fileJson.getAsJsonObject().get("fileID").getAsInt();
                        String s = getSlug(p);
                        if (!slugExcludes.contains(s)) {
                            configuration.getDependencies().add(this.createDependency(curseArtifact(s, p, f)));
                        }
                    }
                }
                return this.project.getDependencies().create(configuration);
            } catch (IOException e) {
                throw new IllegalStateException("Can't create curse ModPack dependency: " + e.getMessage(), e);
            }
        }
    }

    private Dependency createDependency(Object obj) {
        if (this.ext == null) {
            return this.project.getDependencies().create(obj);
        } else {
            return this.ext.deobf(obj);
        }
    }
    
    private static String curseArtifact(String slug, int projectId, int fileId) {
        return "curse.maven:" + slug + "-" + projectId + ":" + fileId;
    }

    private static String getSlug(int projectId) {
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
