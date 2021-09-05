package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev;

import groovy.lang.GroovyObjectSupport;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MultiMCExtension extends GroovyObjectSupport {
    
    private Path basePath = null;
    private String instanceName = null;
    
    public Path getInstancePath() {
        if (this.basePath == null) throw new IllegalStateException("Can't get MultiMC instance path: base path not set.");
        if (this.instanceName == null) throw new IllegalStateException("Can't get MultiMC instance path: instance name not set.");
        return this.basePath.resolve("instances").resolve(this.instanceName).normalize();
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
