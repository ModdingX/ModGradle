package io.github.noeppi_noeppi.tools.modgradle.plugins.cursedep;

import com.google.common.collect.Sets;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
        return this.createDependency(CurseDepPlugin.curseArtifact(projectId, fileId));
    }

    public Dependency pack(int projectId, int fileId) {
        return this.pack(projectId, fileId, Collections.emptyList());
    }

    public Dependency pack(int projectId, int fileId, List<Object> excludes) {
        Set<Integer> idExcludes = Sets.newHashSet();
        Set<String> slugExcludes = Sets.newHashSet();

        for (Object exclude : excludes) {
            if (exclude instanceof Integer id) {
                idExcludes.add(id);
            } else if (exclude instanceof String slug) {
                slugExcludes.add(slug);
            } else {
                throw new IllegalStateException("Cannot add curse ModPack dependency: Excludes must be int (project id) or String (slug). Currently: " + exclude.getClass().getName());
            }
        }

        String configName = "cursepack_" + projectId + "_" + fileId + "_" + idExcludes.stream().sorted().map(Object::toString).collect(Collectors.joining("_")) + "_" + slugExcludes.stream().sorted().collect(Collectors.joining("_"));
        Configuration cache = this.project.getConfigurations().findByName(configName);
        if (cache != null) {
            return this.project.getDependencies().create(cache);
        }

        File file = MavenArtifactDownloader.download(this.project, CurseDepPlugin.curseArtifact("O", projectId, fileId, "zip"), false);
        if (file == null) {
            throw new IllegalStateException("Cannot create curse ModPack dependency: Failed to download manifest");
        } else try {
            ZipFile zipFile = new ZipFile(file);
            ZipEntry entry = zipFile.getEntry("manifest.json");
            if (entry == null) entry = zipFile.getEntry("/manifest.json");
            if (entry == null) {
                throw new IllegalStateException("Cannot create curse ModPack dependency: Pack file contains no manifest");
            }

            InputStreamReader reader = new InputStreamReader(zipFile.getInputStream(entry));
            JsonElement json = ModGradle.GSON.fromJson(reader, JsonElement.class);
            reader.close();
            Configuration config = this.project.getConfigurations().create(configName);

            for (JsonElement fileJson : json.getAsJsonObject().get("files").getAsJsonArray()) {
                int p = fileJson.getAsJsonObject().get("projectID").getAsInt();
                if (!idExcludes.contains(p)) {
                    int f = fileJson.getAsJsonObject().get("fileID").getAsInt();
                    String slug = CurseDepPlugin.getSlug(p);
                    if (!slugExcludes.contains(slug)) {
                        config.getDependencies().add(this.createDependency(CurseDepPlugin.curseArtifact(slug, p, f)));
                    }
                }
            }

            return this.project.getDependencies().create(config);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create curse ModPack dependency: " + e.getMessage(), e);
        }
    }

    private Dependency createDependency(Object obj) {
        if (this.ext == null) {
            return this.project.getDependencies().create(obj);
        }

        return this.ext.deobf(obj);
    }
}
