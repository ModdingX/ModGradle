package org.moddingx.modgradle.plugins.packdev;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.transform.Internal;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.moddingx.modgradle.plugins.packdev.api.CurseProperties;
import org.moddingx.modgradle.util.McEnv;

import javax.annotation.Nullable;
import java.util.*;

public class PackDevExtension extends GroovyObjectSupport {

    public static final String EXTENSION_NAME = "modpack";

    private final Project project;
    private final TargetBuilder targets;
    
    @Nullable
    private String author;

    public PackDevExtension(Project project) {
        this.project = project;
        this.targets = new TargetBuilder();
        this.author = null;
    }

    public void author(String author) {
        this.author = author;
    }
    
    public void targets(Closure<?> closure) {
        closure.setDelegate(this.targets);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        if (closure.getMaximumNumberOfParameters() == 0) {
            closure.call();
        } else {
            closure.call(this.targets);
        }
    }
    
    public void targets(Action<TargetBuilder> action) {
        action.execute(this.targets);
    }
    
    @Internal
    public PackSettings getSettings() {
        String minecraft = McEnv.findMinecraftVersion(this.project).get();
        String forge = McEnv.findForgeVersion(this.project).get();
        return new PackSettings(
                Objects.requireNonNull(this.project.getName(), "Project name not set."),
                Objects.requireNonNull(this.project.getVersion(), "Project version not set.").toString(),
                Objects.requireNonNull(minecraft, "Minecraft version not set."),
                Objects.requireNonNull(forge, "Forge version not set."),
                Optional.ofNullable(this.author)
        );
    }

    @Internal
    public Map<String, Optional<Object>> getAllTargets() {
        return Map.copyOf(this.targets.targets);
    }
    
    public static class TargetBuilder {
        
        private final Map<String, Optional<Object>> targets;

        private TargetBuilder() {
            this.targets = new HashMap<>();
        }

        public void curse(int projectId) {
            this.target("curse", new CurseProperties(projectId));
        }
        
        public void modrinth() {
            this.target("modrinth");
        }
        
        public void server() {
            this.target("server");
        }
        
        public void multimc() {
            this.target("multimc");
        }

        public void target(String id) {
            this.target(id, null);
        }
        
        public void target(String id, Object properties) {
            if (this.targets.containsKey(id)) {
                throw new IllegalArgumentException("Each target can only be built once: " + id);
            } else {
                this.targets.put(id, Optional.ofNullable(properties));
            }
        }
    }
}
