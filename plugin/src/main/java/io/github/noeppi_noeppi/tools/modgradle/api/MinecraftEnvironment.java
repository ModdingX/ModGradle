package io.github.noeppi_noeppi.tools.modgradle.api;

import io.github.noeppi_noeppi.tools.modgradle.util.McEnv;
import org.gradle.api.Project;

/**
 * Used to query data about the minecraft environment
 */
public class MinecraftEnvironment {

    /**
     * Gets the minecraft version used in a project.
     */
    public static String getMinecraft(Project project) {
        return McEnv.findMinecraftVersion(project);
    }
    
    /**
     * Gets the forge version used in a project. Only works in userdev environments.
     */
    public static String getForge(Project project) {
        return McEnv.findForgeVersion(project);
    }
}
