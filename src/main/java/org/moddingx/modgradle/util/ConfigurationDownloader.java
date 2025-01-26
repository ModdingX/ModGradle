package org.moddingx.modgradle.util;

import jakarta.annotation.Nullable;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.moddingx.launcherlib.util.Artifact;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Manifest;

public class ConfigurationDownloader {

    private static final AtomicInteger CONFIGURATION_ID = new AtomicInteger(0);
    
    @Nullable
    public static Executable executable(Project project, Artifact artifact) {
        return executable(project, GradleDependencyUtils.getDependency(project, artifact));
    }
    
    @Nullable
    public static FileCollection download(Project project, Artifact artifact) {
        return download(project, GradleDependencyUtils.getDependency(project, artifact));
    }
    
    @Nullable
    public static FileCollection download(Project project, Artifact artifact, Action<Configuration> action) {
        return download(project, GradleDependencyUtils.getDependency(project, artifact), action);
    }
    
    @Nullable
    public static Executable executable(Project project, Dependency dependency) {
        FileCollection files = download(project, dependency);
        if (files == null) return null;
        // Need to find out main class:
        // We download the main file again without any dependencies
        FileCollection mainFiles = download(project, dependency, configuration -> configuration.setTransitive(false));
        if (mainFiles == null) return null;
        Set<File> mainFileSet = mainFiles.getFiles();
        if (mainFileSet.isEmpty()) throw new IllegalStateException("Dependency resolved to nothing: " + GradleDependencyUtils.toString(dependency)); 
        if (mainFileSet.size() > 1) throw new IllegalStateException("Dependency resolved to more than one element: " + GradleDependencyUtils.toString(dependency)); 
        String mainClass = mainClass(mainFileSet.iterator().next().toPath());
        if (mainClass == null) throw new IllegalStateException("No main class found in dependency: " + GradleDependencyUtils.toString(dependency));
        return new Executable(mainClass, files);
    }

    @Nullable
    public static FileCollection download(Project project, Dependency dependency) {
        return download(project, dependency, configuration -> {});
    }
    
    @Nullable
    public static FileCollection download(Project project, Dependency dependency, Action<Configuration> action) {
        Configuration configuration = project.getConfigurations().create("modgradle_cfg_" + CONFIGURATION_ID.getAndIncrement());
        configuration.getDependencies().add(dependency);
        configuration.resolutionStrategy(resolution -> {
            resolution.cacheChangingModulesFor(10, TimeUnit.MINUTES);
            resolution.cacheDynamicVersionsFor(10, TimeUnit.MINUTES);
        });
        action.execute(configuration);
        FileCollection files = download(project, configuration, GradleDependencyUtils.toString(dependency));
        project.getConfigurations().remove(configuration);
        return files;
    }
    
    @Nullable
    private static FileCollection download(Project project, Configuration configuration, String name) {
        if (!project.getState().getExecuted()) {
            throw new IllegalStateException("Can't download configuration " + configuration + " during evaluation.");
        }
        Set<File> files;
        try {
            files = configuration.resolve();
        } catch (NullPointerException e) {
            //noinspection CallToPrintStackTrace
            new IllegalStateException("Failed to resolve configuration for " + name, e).printStackTrace();
            return null;
        }
        ConfigurableFileCollection fc = project.files();
        files.forEach(fc::from);
        return fc;
    }
    
    public record Executable(String mainClass, FileCollection classpath) {}
    
    @Nullable
    public static String mainClass(Path jarFile) {
        try (FileSystem fs = FileSystems.newFileSystem(jarFile, (ClassLoader) null)) {
            Path manifestPath = fs.getPath("/META-INF/MANIFEST.MF");
            if (Files.isRegularFile(manifestPath)) {
                try (InputStream in = Files.newInputStream(manifestPath)) {
                    Manifest manifest = new Manifest(in);
                    // containsKey does not work
                    Object value = manifest.getMainAttributes().getValue("Main-Class");
                    if (value != null) {
                        return value.toString().strip();
                    }
                }
            }

            Path modulePath = fs.getPath("/module-info.class");
            if (Files.isRegularFile(modulePath)) {
                try (InputStream in = Files.newInputStream(modulePath)) {
                    ModuleDescriptor module = ModuleDescriptor.read(in);
                    if (module.mainClass().isPresent()) {
                        return module.mainClass().get();
                    }
                }
            }

            return null;
        } catch (IOException e) {
            throw new RuntimeException("Could not determine main class", e);
        }
    }
}
