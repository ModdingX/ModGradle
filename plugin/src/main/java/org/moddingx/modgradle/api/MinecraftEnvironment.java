package org.moddingx.modgradle.api;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.moddingx.modgradle.util.McEnv;

/**
 * Used to query data about the minecraft environment
 */
public class MinecraftEnvironment {

    /**
     * Gets the minecraft version used in a project.
     */
    public static Provider<String> getMinecraft(Project project) {
        return McEnv.findMinecraftVersion(project);
    }
    
    /**
     * Gets the forge version used in a project. Only works in userdev environments.
     */
    public static Provider<String> getForge(Project project) {
        return McEnv.findForgeVersion(project);
    }
}
