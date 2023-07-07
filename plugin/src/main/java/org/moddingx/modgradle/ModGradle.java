package org.moddingx.modgradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor;
import org.gradle.api.invocation.Gradle;
import org.moddingx.modgradle.api.ModGradleExtension;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ModGradle {

    // Target java version for ModGradle and external tools
    // not for the toolchain
    public static final int TARGET_JAVA = 17;

    public static final String SOURCE_TRANSFORM = "org.moddingx:SourceTransform:2.1.3";
    public static final String DOCLET_META = "org.moddingx:JavaDocletMeta:1.1.0";

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
            
            project.getExtensions().create(ModGradleExtension.EXTENSION_NAME, ModGradleExtension.class);
            
            project.getRepositories().maven(r -> {
                r.setUrl("https://maven.moddingx.org");
                r.content(c -> includeAll(c, "org.moddingx"));
            });
            
            // Forge Maven is required for dependencies like SrgUtils
            project.getRepositories().maven(r -> {
                r.setUrl("https://maven.minecraftforge.net");
                r.content(c -> {
                    includeAll(c, "cpw.mods");
                    includeAll(c, "de.oceanlabs");
                    includeAll(c, "net.minecraftforge");
                    includeAll(c, "org.mcmodlauncher");
                });
            });
            
            // Parchment Maven is required for dependencies like Feather
            project.getRepositories().maven(r -> {
                r.setUrl("https://maven.parchmentmc.org");
                r.content(c -> includeAll(c, "org.parchmentmc"));
            });
            
            // Required for dependencies
            project.getRepositories().mavenCentral();
        }
    }
    
    private static void includeAll(RepositoryContentDescriptor content, String group) {
        content.includeGroupByRegex(Pattern.quote(group) + "(?:\\..+)?");
    }
    
    @Nullable
    public static Gradle gradle() {
        return gradleInstance;
    }
}
