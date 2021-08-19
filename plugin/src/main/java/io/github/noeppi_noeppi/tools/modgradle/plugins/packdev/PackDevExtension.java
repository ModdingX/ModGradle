package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev;

import groovy.lang.GroovyObjectSupport;
import io.github.noeppi_noeppi.tools.modgradle.util.McEnv;
import org.gradle.api.Project;

import java.util.*;

public class PackDevExtension extends GroovyObjectSupport {
    
    public static final String EXTENSION_NAME = "modpack";

    private final Project project;
    
    private int projectId = -1;
    private String author;

    public PackDevExtension(Project project) {
        this.project = project;
    }

    public void projectId(int projectId) {
        this.projectId = projectId;
    }
    
    public void author(String author) {
        this.author = author;
    }
    
    public PackSettings getSettings() {
        String minecraft = McEnv.findMinecraftVersion(this.project);
        String forge = McEnv.findForgeVersion(this.project);
        if (this.projectId < 0) throw new IllegalStateException("Curse project id not set.");
        return new PackSettings(
                Objects.requireNonNull(minecraft, "Minecraft version not set."),
                Objects.requireNonNull(forge, "Forge version not set."),
                this.projectId, Optional.ofNullable(this.author)
        );
    }
}
