package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate;

import groovy.transform.Internal;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import org.gradle.api.Project;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class McUpdateExtension {

    public static final String EXTENSION_NAME = "mcupdate";

    private final Project project;

    private String mcv = null;
    private URL configuration = null;
    
    private String tool = null;
    private final Set<URL> additionalMappings;

    public McUpdateExtension(Project project) {
        this.project = project;
        this.additionalMappings = new HashSet<>();
    }

    public void version(String mcv) {
        this.mcv = mcv;
    }
    
    public void configuration(String configuration) {
        this.configuration(toURL(configuration));
        
    }
    
    public void configuration(File configuration) {
        this.configuration(toURL(configuration));
    }
        
    public void configuration(URL configuration) {
        this.configuration = configuration;
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
    public URL getConfiguration() throws MalformedURLException {
        return this.configuration == null ? new URL("https://noeppi-noeppi.github.io/MinecraftUtilities/" + this.getMinecraft() + "/mcupdate.json") : this.configuration;
    }
    
    @Internal
    public String getTool() {
        return this.tool == null ? ModGradle.SOURCE_TRANSFORM : this.tool;
    }
    
    @Internal
    public Set<URL> getAdditionalMappings() {
        return Collections.unmodifiableSet(this.additionalMappings);
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
