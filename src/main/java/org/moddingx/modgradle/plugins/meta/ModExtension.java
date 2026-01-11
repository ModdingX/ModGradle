package org.moddingx.modgradle.plugins.meta;

import groovy.lang.*;
import jakarta.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.process.ExecOperations;
import org.moddingx.launcherlib.util.LazyValue;
import org.moddingx.modgradle.plugins.meta.setup.ModBuildSetup;
import org.moddingx.modgradle.plugins.meta.delegate.ModConfig;
import org.moddingx.modgradle.plugins.meta.setup.ProjectContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class ModExtension extends GroovyObjectSupport {

    private final Project project;
    private final ExecOperations execOps;
    private final ModPropertyAccess propertyAccess;

    @Nullable
    private Map<String, Object> properties;

    public ModExtension(Project project, ExecOperations execOps) {
        this.project = project;
        this.execOps = execOps;
        this.properties = null;
        this.propertyAccess = new ModPropertyAccess(this);
    }

    public void configure(@DelegatesTo(value = ModConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws IOException {
        if (this.properties != null) throw new IllegalStateException("You can only have a single mod.configure block in your build.");
        ModConfig config = ModConfig.configure(closure, new ModConfig(), ModConfig::delegate);
        this.properties = new HashMap<>();
        try {
            ModBuildSetup.configureBuild(new ProjectContext(this.project, this.execOps, this.propertyAccess, this.properties::put, this::dependsOnProperties), config);
        } catch (RuntimeException e) {
            throw new RuntimeException("Mod configuration failed.", e);
        }
    }

    public ModPropertyAccess getSetup() {
        return this.propertyAccess;
    }
    
    public Object getAt(String propertyName) { // this[propertyName]
        if (this.properties != null) {
            if ("group".equals(propertyName)) return this.project.getGroup();
            if ("name".equals(propertyName)) return this.project.getName();
            if ("version".equals(propertyName)) return this.project.getVersion();
            if (this.properties.containsKey(propertyName)) {
                Object value = this.properties.get(propertyName);
                return value instanceof LazyValue<?> lazy ? lazy.get() : value;
            }
        }
        throw new NoSuchElementException("mod." + propertyName);
    }
    
    public boolean isCase(String propertyName) { // propertyName in this
        if (this.properties == null) return false;
        return "group".equals(propertyName) || "name".equals(propertyName) || "version".equals(propertyName) || this.properties.containsKey(propertyName);
    }

    private void dependsOnProperties(TaskProvider<?> taskProvider) {
        this.project.afterEvaluate(_ -> {
            if (this.properties != null) {
                taskProvider.configure(task -> {
                    if (this.project.getGroup() instanceof String group) {
                        task.getInputs().property("modgradle_mod_property_group", group);
                    }
                    if (this.project.getVersion() instanceof String version) {
                        task.getInputs().property("modgradle_mod_property_version", version);
                    }
                    task.getInputs().property("modgradle_mod_property_name", this.project.getName());
                    for (Map.Entry<String, Object> property : this.properties.entrySet()) {
                        if (!(property.getValue() instanceof LazyValue<?>) && property.getValue() instanceof Serializable && !(property instanceof Closure<?>)) {
                            task.getInputs().property("modgradle_mod_property_" + property.getKey(), property.getValue());
                        }
                    }
                });
            }
        });
    }
}
