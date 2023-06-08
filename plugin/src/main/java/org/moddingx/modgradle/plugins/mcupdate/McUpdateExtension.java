package org.moddingx.modgradle.plugins.mcupdate;

import groovy.transform.Internal;
import org.gradle.api.Project;
import org.moddingx.modgradle.ModGradle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

public class McUpdateExtension {

    public static final String EXTENSION_NAME = "mcupdate";

    private final Project project;

    private String mcv = null;
    private URL config = null;
    
    private String tool = null;
    private final List<URL> additionalMappings;

    public McUpdateExtension(Project project) {
        this.project = project;
        this.additionalMappings = new ArrayList<>();
    }

    public void version(String mcv) {
        this.mcv = mcv;
    }
    
    public void config(String config) {
        this.config(toURL(config));
        
    }
    
    public void config(File config) {
        this.config(toURL(config));
    }
        
    public void config(URL config) {
        this.config = config;
    }
    
    public void tool(String tool) {
        this.tool = tool;
    }

    public void mappings(String mappings) {
        this.mappings(toURL(mappings));
    }
    
    public void mappings(File mappings) {
        this.mappings(toURL(mappings));
    }
    
    public void mappings(URL mappings) {
        this.additionalMappings.add(mappings);
    }
    
    @Internal
    public String getMinecraft() {
        if (this.mcv == null) {
            throw new IllegalStateException("Target version for mcupdate not set.");
        } else {
            return this.mcv;
        }
    }
    
    @Internal
    public URL getConfig() throws MalformedURLException {
        return this.config == null ? new URL("https://assets.moddingx.org/mcupdate/" + this.getMinecraft() + "/mcupdate.json") : this.config;
    }
    
    @Internal
    public String getTool() {
        return this.tool == null ? ModGradle.SOURCE_TRANSFORM : this.tool;
    }
    
    @Internal
    public List<URL> getAdditionalMappings() {
        return List.copyOf(this.additionalMappings);
    }
    
    private static URL toURL(String string) {
        try {
            return new URL(string);
        } catch (MalformedURLException e) {
            try {
                return Paths.get(string).toAbsolutePath().normalize().toUri().toURL();
            } catch (MalformedURLException ex) {
                throw new IllegalArgumentException("Can't convert string to URL: " + string, e);
            }
        }
    }
    
    private static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Can't convert file to URL: " + file, e);
        }
    }
}
