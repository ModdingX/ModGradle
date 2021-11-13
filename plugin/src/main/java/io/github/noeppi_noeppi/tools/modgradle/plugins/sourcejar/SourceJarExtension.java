package io.github.noeppi_noeppi.tools.modgradle.plugins.sourcejar;

import groovy.lang.GroovyObjectSupport;
import groovy.transform.Internal;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SourceJarExtension extends GroovyObjectSupport {

    public static final String EXTENSION_NAME = "sourceJarConfigure";

    private final Project project;
    
    private final Set<File> additionalSources;
    private final ConfigurableFileCollection additionalClasspath;

    public SourceJarExtension(Project project) {
        this.project = project;
        this.additionalSources = new HashSet<>();
        this.additionalClasspath = project.files();
    }

    public void additionalSourceDir(String dir) {
        this.additionalSourceDir(this.project.file(dir));
    }
    
    public void additionalSourceDir(File dir) {
        this.additionalSources.add(dir);
    }
    
    public void additionalClasspath(FileCollection cp) {
        this.additionalClasspath.from(cp);
    }
    
    @Internal
    public Set<File> getAdditionalSources() {
        return Collections.unmodifiableSet(this.additionalSources);
    }
    
    @Internal
    public FileCollection getAdditionalClasspath() {
        return this.additionalClasspath;
    }
}
