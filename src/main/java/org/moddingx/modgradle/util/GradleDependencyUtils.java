package org.moddingx.modgradle.util;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.moddingx.launcherlib.util.Artifact;

public class GradleDependencyUtils {
    
    public static ExternalModuleDependency getDependency(Project project, Artifact artifact) {
        return (ExternalModuleDependency) project.getDependencies().create(artifact.getDescriptor());
    }
    
    public static String toString(Dependency dependency) {
        if (dependency instanceof ModuleDependency moduleDependency) {
            return moduleDependency.getGroup() + ":" + moduleDependency.getName() + ":" + moduleDependency.getVersion();
        } else {
            return dependency.toString();
        }
    }
}
