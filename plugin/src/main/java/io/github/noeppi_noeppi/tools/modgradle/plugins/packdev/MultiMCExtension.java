package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev;

import groovy.lang.GroovyObjectSupport;
import groovy.transform.Internal;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MultiMCExtension extends GroovyObjectSupport {
    
    private Path basePath = null;
    private String instanceName = null;
    
    @Internal
    public Path getInstancePath(Project project) {
        if (this.basePath == null) throw new IllegalStateException("Can't get MultiMC instance path: base path not set.");
        String instance = this.instanceName == null ? project.getName() : this.instanceName;
        return this.basePath.resolve("instances").resolve(instance).normalize();
    }
    
    public void path(String path) {
        this.path(Paths.get(path));
    }
    
    public void path(File path) {
        this.path(path.toPath());
    }
    
    public void path(Path path) {
        this.basePath = path.toAbsolutePath().normalize();
    }
    
    public void instance(String name) {
        this.instanceName = name;
    }
}
