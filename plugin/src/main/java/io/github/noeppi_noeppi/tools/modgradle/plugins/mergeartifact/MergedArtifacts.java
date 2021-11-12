package io.github.noeppi_noeppi.tools.modgradle.plugins.mergeartifact;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.gradle.common.util.MavenArtifactDownloader;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MergedArtifacts {

    private static final Map<Project, Set<File>> generatedSources = new HashMap<>();
    private static final Map<Project, File> generatedServices = new HashMap<>();
    
    public static Set<FileTree> getShadeFiles(Project project) {
        MergeArtifactExtension ext = project.getExtensions().findByType(MergeArtifactExtension.class);
        if (ext == null) return Set.of();
        ImmutableSet.Builder<FileTree> set = ImmutableSet.builder();
        for (String artifact : ext.getArtifacts()) {
            File file = MavenArtifactDownloader.download(project, artifact, false);
            if (file == null) throw new IllegalStateException("Artifact for shade not found: " + artifact);
            set.add(project.zipTree(file).matching(filter -> filter.exclude("META-INF/**")));
        }
        return set.build();
    }

    public static synchronized Set<File> additionalSourceDirs(Project project) {
        if (generatedSources.containsKey(project)) return generatedSources.get(project);
        try {
            MergeArtifactExtension ext = project.getExtensions().findByType(MergeArtifactExtension.class);
            if (ext == null) return Set.of();
            ImmutableSet.Builder<File> set = ImmutableSet.builder();
            for (String artifact : ext.getIncluded()) {
                File file = null;
                try {
                    file = MavenArtifactDownloader.download(project, artifact + ":sources", false);
                } catch (Exception e) {
                    //
                }
                if (file != null) {
                    File target = project.file("build/mergeArtifactsSources/" + artifact.replace("/", "").replace("\\", ""));
                    if (Files.exists(target.toPath())) PathUtils.deleteDirectory(target.toPath());
                    Files.createDirectories(target.toPath());
                    try (FileSystem fs = FSUtil.getFileSystem(URI.create("jar:" + file.toPath().toUri()))) {
                        PathUtils.copyDirectory(fs.getPath(""), target.toPath());
                    }
                    set.add(target);
                }
            }
            generatedSources.put(project, set.build());
            return generatedSources.get(project);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized File mergeServiceFiles(Project project, FileCollection resourceDirs) {
        if (generatedServices.containsKey(project)) return generatedServices.get(project);
        try {
            Map<String, Set<String>> services = new HashMap<>();
            for (File file : resourceDirs.getFiles()) {
                Path path = file.toPath().resolve("META-INF/services");
                if (Files.isDirectory(path)) {
                    for (Path p : Files.list(path).toList()) {
                        Set<String> set = services.computeIfAbsent(p.getFileName().toString(), k -> new HashSet<>());
                        Files.readAllLines(p).stream().map(String::strip).filter(s -> !s.isEmpty()).forEach(set::add);
                    }
                }
            }
            MergeArtifactExtension ext = project.getExtensions().findByType(MergeArtifactExtension.class);
            if (ext != null) {
                for (String artifact : ext.getArtifacts()) {
                    File file = MavenArtifactDownloader.download(project, artifact, false);
                    if (file == null) throw new IllegalStateException("Artifact for shade not found: " + artifact);
                    try (FileSystem fs = FSUtil.getFileSystem(URI.create("jar:" + file.toPath().toAbsolutePath().normalize().toUri()))) {
                        Path path = fs.getPath("META-INF/services");
                        if (Files.isDirectory(path)) {
                            for (Path p : Files.list(path).toList()) {
                                Set<String> set = services.computeIfAbsent(p.getFileName().toString(), k -> new HashSet<>());
                                Files.readAllLines(p).stream().map(String::strip).filter(s -> !s.isEmpty()).forEach(set::add);
                            }
                        }
                    }
                }
            }
            Path basePath = project.file("build/mergeArtifactsServices").toPath();
            if (Files.exists(basePath)) PathUtils.deleteDirectory(basePath);
            Files.createDirectories(basePath);
            for (Map.Entry<String, Set<String>> entry : services.entrySet()) {
                Path targetPath = basePath.resolve("META-INF/services").resolve(entry.getKey());
                Files.createDirectories(targetPath.getParent());
                Files.writeString(targetPath, entry.getValue().stream().sorted().map(s -> s + "\n").collect(Collectors.joining("")));
            }
            generatedServices.put(project, project.file("build/mergeArtifactsServices"));
            return generatedServices.get(project);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
