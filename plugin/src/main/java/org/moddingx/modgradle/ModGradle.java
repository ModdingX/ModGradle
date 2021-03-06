package org.moddingx.modgradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class ModGradle {

    // Target java version for ModGradle and external tools
    // not for the toolchain
    public static final int TARGET_JAVA = 17;

    public static final String SOURCE_TRANSFORM = "org.moddingx:SourceTransform:2.0.0";
    public static final String DOCLET_META = "org.moddingx:JavaDocletMeta:1.0.0";

    public static final Gson GSON;
    public static final Gson INTERNAL;
    
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        builder.setLenient();
        builder.setPrettyPrinting();
        GSON = builder.create();
    }

    static {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        INTERNAL = builder.create();
    }

    private static final Set<Project> initialised = new HashSet<>();
    private static Gradle gradleInstance = null;
    
    public static synchronized void initialiseProject(Project project) {
        if (!initialised.contains(project)) {
            if (gradleInstance == null) {
                gradleInstance = project.getGradle();
            }
            
            initialised.add(project);
            
            project.getRepositories().maven(r -> {
                r.setUrl("https://maven.moddingx.org");
                r.content(c -> c.includeGroup("org.moddingx"));
            });
            
            // Forge Maven is required for dependencies like SrgUtils
            project.getRepositories().maven(r -> {
                r.setUrl("https://maven.minecraftforge.net");
                r.content(c -> c.includeGroup("net.minecraftforge"));
            });
            
            // Parchment Maven is required for dependencies like Feather
            project.getRepositories().maven(r -> {
                r.setUrl("https://maven.parchmentmc.org");
                r.content(c -> {
                    c.includeGroup("org.parchmentmc");
                    c.includeGroup("org.parchmentmc.feather");
                    c.excludeGroup("org.parchmentmc.data");
                });
            });
            
            // Required for dependencies
            project.getRepositories().mavenCentral();
        }
    }
    
    @Nullable
    public static Gradle gradle() {
        return gradleInstance;
    }
}
