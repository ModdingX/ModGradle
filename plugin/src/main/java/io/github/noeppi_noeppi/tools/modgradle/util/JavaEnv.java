package io.github.noeppi_noeppi.tools.modgradle.util;

import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import java.util.ArrayList;

public class JavaEnv {
    
    public static JavaPluginExtension getJavaExtension(Project project) {
        JavaPluginExtension ext = project.getExtensions().findByType(JavaPluginExtension.class);
        if (ext == null) throw new IllegalStateException("Java plugin extension not found. Is the java plugin applied?");
        return ext;
    }
    
    public static SourceSet getJavaSources(Project project) {
        try {
            return getJavaExtension(project).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        } catch (UnknownDomainObjectException e) {
            throw new IllegalStateException("No `main` source set in java extension.", e);
        }
    }
    
    public static FileCollection getJavaSourceDirs(Project project) {
        SourceSet sources = getJavaSources(project);
        return project.files(new ArrayList<>(sources.getJava().getSrcDirs()).toArray());
    }
    
    public static FileCollection getJavaResourceDirs(Project project) {
        SourceSet sources = getJavaSources(project);
        return project.files(new ArrayList<>(sources.getResources().getSrcDirs()).toArray());
    }
}
