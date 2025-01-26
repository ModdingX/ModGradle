package org.moddingx.modgradle;

import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.moddingx.launcherlib.launcher.Launcher;
import org.moddingx.modgradle.util.ModGradleVersionAccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class ModGradle {
    
    private static final int CACHE_VERSION = 1;
    
    private static Gradle gradleInstance;
    private static Path cacheBase;
    private static Launcher launcher;
    private static ModGradleVersionAccess versions;
    private static final Set<Project> initialisedProjects = new HashSet<>();

    public static Gradle gradle() {
        return Objects.requireNonNull(gradleInstance, "ModGradle has not yet been initialised.");
    }

    public static Path cacheBase() {
        return Objects.requireNonNull(cacheBase, "ModGradle has not yet been initialised.");
    }

    public static Launcher launcher() {
        return Objects.requireNonNull(launcher, "ModGradle has not yet been initialised.");
    }

    public static ModGradleVersionAccess versions() {
        return Objects.requireNonNull(versions, "ModGradle has not yet been initialised.");
    }
    
    public static synchronized void init(Gradle gradle) {
        if (gradleInstance == null || cacheBase == null || launcher == null || versions == null) {
            gradleInstance = gradle;
            cacheBase = gradle.getGradleUserHomeDir().toPath().resolve("modgradle").resolve("v" + CACHE_VERSION);
            try {
                Files.createDirectories(cacheBase);
            } catch (IOException e) {
                throw new RuntimeException("Could not initialise modgradle cache directory");
            }
            launcher = new Launcher(cacheBase.resolve("launcher"));
            versions = new ModGradleVersionAccess(cacheBase.resolve("versions.json"), launcher);
        }
    }

    public static synchronized void init(Project project) {
        init(project.getGradle());
        if (initialisedProjects.add(project)) {
            project.getRepositories().mavenCentral();
            project.getRepositories().maven(r -> {
                r.setName("ModdingX maven");
                r.setUrl("https://maven.moddingx.org/release");
                r.content(c -> c.includeGroupByRegex(Pattern.quote("org.moddingx") + "(?:\\..+)?"));
            });
        }
    }
}
