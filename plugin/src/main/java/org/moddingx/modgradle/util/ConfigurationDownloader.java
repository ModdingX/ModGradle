package org.moddingx.modgradle.util;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.moddingx.modgradle.util.java.JarUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConfigurationDownloader {

    private static final AtomicInteger CONFIGURATION_ID = new AtomicInteger(0);
    
    @Nullable
    public static Executable executable(Project project, String dependency) {
        return executable(project, project.getDependencies().create(dependency));
    }
    
    @Nullable
    public static FileCollection download(Project project, String dependency) {
        return download(project, project.getDependencies().create(dependency));
    }
    
    @Nullable
    public static FileCollection download(Project project, String dependency, Action<Configuration> action) {
        return download(project, project.getDependencies().create(dependency), action);
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
        if (mainFileSet.isEmpty()) throw new IllegalStateException("Dependency resolved to nothing: " + MgUtil.dependencyName(dependency)); 
        if (mainFileSet.size() > 1) throw new IllegalStateException("Dependency resolved to more than one element: " + MgUtil.dependencyName(dependency)); 
        String mainClass = JarUtil.mainClass(mainFileSet.iterator().next());
        if (mainClass == null) throw new IllegalStateException("No main class found in dependency: " + MgUtil.dependencyName(dependency));
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
        FileCollection files = download(project, configuration, MgUtil.dependencyName(dependency));
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
            // According to forge, this might sometimes happen.
            new IllegalStateException("Failed to resolve configuration for " + name, e).printStackTrace();
            return null;
        }
        ConfigurableFileCollection fc = project.files();
        files.forEach(fc::from);
        return fc;
    }
    
    public record Executable(String mainClass, FileCollection classpath) {}
}
