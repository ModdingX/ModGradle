package io.github.noeppi_noeppi.tools.modgradle.api;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.util.StringUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.*;
import org.gradle.api.Project;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Utilities for versioning and to get data for a minecraft version..
 */
public class Versioning {

    private static final List<Pair<VersionRange, VersionInfo>> VERSION_MAP;

    static {
        try {
            VERSION_MAP = List.of(
                    Pair.of(VersionRange.createFromVersionSpec("(,1.9)"), new VersionInfo(8, 1, OptionalInt.empty(), null)),
                    Pair.of(VersionRange.createFromVersionSpec("[1.9,1.11)"), new VersionInfo(8, 2, OptionalInt.empty(), null)),
                    Pair.of(VersionRange.createFromVersionSpec("[1.11,1.12)"), new VersionInfo(8, 3, OptionalInt.empty(), null)),
                    Pair.of(VersionRange.createFromVersionSpec("[1.12,1.13)"), new VersionInfo(8, 3, OptionalInt.empty(), null)),
                    Pair.of(VersionRange.createFromVersionSpec("[1.13,1.15)"), new VersionInfo(8, 4, OptionalInt.of(4), null)),
                    Pair.of(VersionRange.createFromVersionSpec("[1.15,1.16)"), new VersionInfo(8, 5, OptionalInt.of(5), null)),
                    Pair.of(VersionRange.createFromVersionSpec("[1.16,1.16.2)"), new VersionInfo(8, 5, OptionalInt.of(5), new MixinVersion("JAVA_8", "0.8.2"))),
                    Pair.of(VersionRange.createFromVersionSpec("[1.16.2,1.17)"), new VersionInfo(8, 6, OptionalInt.of(6), new MixinVersion("JAVA_8", "0.8.2"))),
                    Pair.of(VersionRange.createFromVersionSpec("[1.17,1.18)"), new VersionInfo(16, 7, OptionalInt.of(7), new MixinVersion("JAVA_16", "0.8.4"))),
                    Pair.of(VersionRange.createFromVersionSpec("[1.18,1.18.2)"), new VersionInfo(17, 8, OptionalInt.of(8), new MixinVersion("JAVA_17", "0.8.5"))),
                    Pair.of(VersionRange.createFromVersionSpec("[1.18.2,1.19)"), new VersionInfo(17, 8, OptionalInt.of(9), new MixinVersion("JAVA_17", "0.8.5")))
            );
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the current project build version based on the files in a maven repository.
     * @param baseVersion The project version without the release number.
     * @param localMaven The local path of the maven repository.
     */
    public static String getVersion(Project project, String baseVersion, String localMaven) {
        try {
            String group = project.getGroup().toString();
            String artifact = project.getName();
            if (group.isEmpty()) throw new IllegalStateException("Can't get version: Group not set.");
            if (artifact.isEmpty()) throw new IllegalStateException("Can't get version: Artifact not set.");
            Path mavenPath = project.file(localMaven).toPath().resolve(group.replace('.', '/')).resolve(artifact);
            if (!Files.isDirectory(mavenPath)) {
                return baseVersion + ".0";
            }
            return baseVersion + "." + Files.walk(mavenPath)
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".pom"))
                    .map(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.substring(fileName.indexOf('-', artifact.length()) + 1, fileName.length() - 4);
                    })
                    .filter(version -> version.startsWith(baseVersion))
                    .max(Comparator.comparing(ComparableVersion::new))
                    .map(ver -> ver.substring(StringUtil.lastIndexWhere(ver, chr -> "0123456789".indexOf(chr) < 0) + 1))
                    .map(ver -> ver.isEmpty() ? "-1" : ver)
                    .map(ver -> Integer.toString(Integer.parseInt(ver) + 1))
                    .orElse("0");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the major java version for a given version of minecraft.
     */
    public static int getJavaVersion(String minecraft) {
        return getMinecraftVersion(minecraft).java();
    }

    /**
     * Gets the resource pack version for a given version of minecraft.
     */
    public static int getResourceVersion(String minecraft) {
        return getMinecraftVersion(minecraft).resource();
    }

    /**
     * Gets the datapack version for a given version of minecraft.
     * If datapacks were not yet introduced in that version, the returned optional will be empty.
     */
    public static OptionalInt getDataVersion(String minecraft) {
        return getMinecraftVersion(minecraft).data();
    }

    /**
     * Gets the mixin version for a given version of minecraft.
     */
    @Nullable
    public static MixinVersion getMixinVersion(String minecraft) {
        return getMinecraftVersion(minecraft).mixin();
    }

    private static VersionInfo getMinecraftVersion(String minecraft) {
        ArtifactVersion v = new DefaultArtifactVersion(minecraft);
        for (Pair<VersionRange, VersionInfo> pair : VERSION_MAP) {
            if (pair.getLeft().containsVersion(v)) {
                return pair.getRight();
            }
        }
        if (ModGradle.TARGET_MINECRAFT.equals(minecraft)) {
            throw new IllegalStateException("Version information missing for default version: " + ModGradle.TARGET_MINECRAFT);
        } else {
            return getMinecraftVersion(ModGradle.TARGET_MINECRAFT);
        }
    }

    private record VersionInfo(int java, int resource, OptionalInt data, @Nullable MixinVersion mixin) {}
}
