package io.github.noeppi_noeppi.tools.modgradle.plugins.mergeartifact;

import groovy.lang.GroovyObjectSupport;
import groovy.transform.Internal;

import java.util.ArrayList;
import java.util.List;

public class MergeArtifactExtension extends GroovyObjectSupport {
    
    public static final String EXTENSION_NAME = "mergeArtifacts";

    private final List<String> artifacts = new ArrayList<>();
    private final List<String> included = new ArrayList<>();

    // Also copied to source jar and javadoc
    public void include(String artifact) {
        if (!this.artifacts.contains(artifact)) this.artifacts.add(artifact);
        if (!this.included.contains(artifact)) this.included.add(artifact);
    }

    public void shade(String artifact) {
        if (!this.artifacts.contains(artifact)) this.artifacts.add(artifact);
    }
    
    @Internal
    public List<String> getArtifacts() {
        return List.copyOf(this.artifacts);
    }

    @Internal
    public List<String> getIncluded() {
        return List.copyOf(this.included);
    }
}
