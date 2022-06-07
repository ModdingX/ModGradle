package org.moddingx.modgradle.plugins.meta;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.moddingx.modgradle.ModGradle;

import javax.annotation.Nonnull;

// Miscellaneous stuff mostly for the use in ModUtils
public class MetaPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        ModGradle.initialiseProject(project);
        SetupTask setup = project.getTasks().create("setup", SetupTask.class);
    }
}
