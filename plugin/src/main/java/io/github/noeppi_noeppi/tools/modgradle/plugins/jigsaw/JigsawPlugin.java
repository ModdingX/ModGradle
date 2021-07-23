package io.github.noeppi_noeppi.tools.modgradle.plugins.jigsaw;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.annotation.Nonnull;

// Allow to generate module descriptors based on another file that
// is more readable and does not need to be expanded all the time.
public class JigsawPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        Task desc = project.getTasks().create("createModuleDescriptor", CreateModuleDescriptorTask.class);
        project.getTasks().withType(JavaCompile.class).forEach(task -> task.dependsOn(desc));
    }
}
