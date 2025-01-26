package org.moddingx.modgradle.plugins.meta.setup;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.moddingx.modgradle.plugins.meta.ModPropertyAccess;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public sealed class ProjectContext permits ModContext {
    
    private final Project project;
    private final ModPropertyAccess properties;
    private final BiConsumer<String, Object> modPropertySetter;
    private final Consumer<TaskProvider<?>> dependsOnProperties;

    protected ProjectContext(ProjectContext context) {
        this(context.project, context.properties, context.modPropertySetter, context.dependsOnProperties);
    }

    public ProjectContext(Project project, ModPropertyAccess properties, BiConsumer<String, Object> modPropertySetter, Consumer<TaskProvider<?>> dependsOnProperties) {
        this.project = project;
        this.properties = properties;
        this.modPropertySetter = modPropertySetter;
        this.dependsOnProperties = dependsOnProperties;
    }

    public Project project() {
        return this.project;
    }

    public ModPropertyAccess properties() {
        return this.properties;
    }
    
    public void modProperty(String name, Object value) {
        this.modPropertySetter.accept(name, value);
    }

    public void dependsOnProperties(TaskProvider<?> task) {
        this.dependsOnProperties.accept(task);
    }
}
