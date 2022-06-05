package io.github.noeppi_noeppi.tools.modgradle.api;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.util.StringUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.versioning.*;
import org.gradle.api.Project;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Utilities for versioning and to get data for a minecraft version..
 */
public class Versioning {

    private static List<Pair<VersionRange, VersionInfo>> VERSION_MAP = null;

    private static List<Pair<VersionRange, VersionInfo>> versionMap() {
        if (VERSION_MAP == null) {
            try {
                URI uri = URI.create("https://assets.melanx.de/minecraft_data.json");
                JsonObject json = ModGradle.GSON.fromJson(new InputStreamReader(uri.toURL().openStream()), JsonObject.class);

                ImmutableList.Builder<Pair<VersionRange, VersionInfo>> builder = ImmutableList.builder();
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    VersionRange versionRange = VersionRange.createFromVersionSpec(entry.getKey());

                    JsonObject versionInfo = entry.getValue().getAsJsonObject();
                    int java = versionInfo.get("java").getAsInt();
                    int resource = versionInfo.get("resource_pack").getAsInt();
                    OptionalInt data = versionInfo.has("data_pack") ? OptionalInt.of(versionInfo.get("data_pack").getAsInt()) : OptionalInt.empty();
                    MixinVersion mixin = null;
                    if (versionInfo.has("mixin")) {
                        JsonObject mixinObj = versionInfo.getAsJsonObject("mixin");
                        String compatibility = mixinObj.get("compatibility").getAsString();
                        String release = mixinObj.get("release").getAsString();
                        mixin = new MixinVersion(compatibility, release);
                    }

                    builder.add(Pair.of(versionRange, new VersionInfo(java, resource, data, mixin)));
                }

                VERSION_MAP = builder.build();
            } catch (IOException | InvalidVersionSpecificationException e) {
                throw new RuntimeException(e);
            }
        }

        return VERSION_MAP;
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
        for (Pair<VersionRange, VersionInfo> pair : versionMap()) {
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
