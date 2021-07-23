package io.github.noeppi_noeppi.tools.modgradle.plugins.meta;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

// Miscellaneous stuff mostly for the use in ModUtils
public class MetaPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        SetupTask setup = project.getTasks().create("setup", SetupTask.class);
    }
}
