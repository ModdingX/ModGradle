package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev;

import com.google.common.collect.ImmutableList;
import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.transform.Internal;
import io.github.noeppi_noeppi.tools.modgradle.util.McEnv;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PackDevExtension extends GroovyObjectSupport {
    
    public static final String EXTENSION_NAME = "modpack";

    private final Project project;
    private final MultiMCExtension multimc;
    private final List<String> editions = new ArrayList<>();
    
    private int projectId = -1;
    private String author;

    public PackDevExtension(Project project) {
        this.project = project;
        this.multimc = new MultiMCExtension();
    }

    public void projectId(int projectId) {
        this.projectId = projectId;
    }
    
    public void author(String author) {
        this.author = author;
    }
    
    public void multimc(Closure<Void> multimc) {
        multimc.rehydrate(multimc.getDelegate(), multimc.getOwner(), this.multimc).call();
    }
    
    public void edition(String edition) {
        this.editions.add(edition);
    }
    
    @Internal
    public PackSettings getSettings() {
        String minecraft = McEnv.findMinecraftVersion(this.project);
        String forge = McEnv.findForgeVersion(this.project);
        if (this.projectId < 0) throw new IllegalStateException("Curse project id not set.");
        return new PackSettings(
                Objects.requireNonNull(minecraft, "Minecraft version not set."),
                Objects.requireNonNull(forge, "Forge version not set."),
                this.projectId, Optional.ofNullable(this.author),
                ImmutableList.copyOf(this.editions)
        );
    }
    
    @Internal
    public MultiMCExtension getMultiMc() {
        return this.multimc;
    }
}
