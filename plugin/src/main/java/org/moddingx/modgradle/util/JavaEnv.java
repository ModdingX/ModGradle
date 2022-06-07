package org.moddingx.modgradle.util;

import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

import java.util.ArrayList;

public class JavaEnv {
    
    public static Provider<JavaPluginExtension> getJavaExtension(Project project) {
        return project.provider(() -> {
            JavaPluginExtension ext = project.getExtensions().findByType(JavaPluginExtension.class);
            if (ext == null)
                throw new IllegalStateException("Java plugin extension not found. Is the java plugin applied?");
            return ext;
        });
    }
    
    public static Provider<SourceSet> getJavaSources(Project project) {
        return getJavaSources(project, SourceSet.MAIN_SOURCE_SET_NAME);
    }

    public static Provider<SourceSet> getJavaSources(Project project, String sourceSet) {
        return getJavaExtension(project).map(ext -> {
            try {
                return ext.getSourceSets().getByName(sourceSet);
            } catch (UnknownDomainObjectException e) {
                throw new IllegalStateException("No " + sourceSet + " source set in java extension.", e);
            }
        });
    }
    
    public static Provider<FileCollection> getJavaSourceDirs(Project project) {
        return getJavaSourceDirs(project, SourceSet.MAIN_SOURCE_SET_NAME);
    }
    
    public static Provider<FileCollection> getJavaSourceDirs(Project project, String sourceSet) {
        return getJavaSources(project, sourceSet).map(s -> project.files(new ArrayList<>(s.getJava().getSrcDirs()).toArray()));
    }
    
    public static Provider<FileCollection> getJavaResourceDirs(Project project) {
        return getJavaResourceDirs(project, SourceSet.MAIN_SOURCE_SET_NAME);
    }
    
    public static Provider<FileCollection> getJavaResourceDirs(Project project, String sourceSet) {
        return getJavaSources(project, sourceSet).map(s -> project.files(new ArrayList<>(s.getResources().getSrcDirs()).toArray()));
    }
}
