package io.github.noeppi_noeppi.tools.modgradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.Project;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ModGradle {
    
    public static final String SOURCE_TRANSFORM = "io.github.noeppi_noeppi.tools:SourceTransform:1.0.12:fatjar";
    public static final String DOCLET_META = "io.github.noeppi_noeppi.tools:JavaDocletMeta:0.0.5:fatjar";

    // Target minecraft version. Acts as default
    // ModGradle can still be used with other minecraft versions
    // For example this is the fallback when using an unknown
    // version in the Versioning class
    public static final String TARGET_MINECRAFT = "1.17.1";

    // Target java version for ModGradle and external tools
    // not for the toolchain
    public static final int TARGET_JAVA = 16;

    @SuppressWarnings("TrivialFunctionalExpressionUsage")
    public static final Gson GSON = ((Supplier<Gson>) () -> {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        builder.setLenient();
        builder.setPrettyPrinting();
        return builder.create();
    }).get();

    @SuppressWarnings("TrivialFunctionalExpressionUsage")
    public static final Gson INTERNAL = ((Supplier<Gson>) () -> {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        return builder.create();
    }).get();
    
    private static final Set<Project> initialised = new HashSet<>();
    
    public static synchronized void initialiseProject(Project project) {
        if (!initialised.contains(project)) {
            initialised.add(project);
            project.getRepositories().maven(r -> {
                r.setUrl("https://noeppi-noeppi.github.io/MinecraftUtilities/maven");
                r.content(c -> c.includeGroup("io.github.noeppi_noeppi.tools"));
            });
        }
    }
}
