package io.github.noeppi_noeppi.tools.modgradle.plugins.meta;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.api.MixinVersion;
import io.github.noeppi_noeppi.tools.modgradle.api.Versioning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModFiles {
    
    public static final Pattern MIN_MIXIN = Pattern.compile("(\\d+\\.\\d+)(?:\\.\\d+)*");
    
    public static void createToml(Path path, String modid, String name, String minecraftVersion, String forgeVersion, String license) throws IOException {
        if (!Files.exists(path)) {
            String loaderVersion;
            String requiredForgeVersion;
            if (forgeVersion.contains(".")) {
                loaderVersion = forgeVersion.substring(0, forgeVersion.indexOf('.'));
                requiredForgeVersion = forgeVersion.substring(0, forgeVersion.lastIndexOf('.'));
            } else {
                loaderVersion = forgeVersion;
                requiredForgeVersion = forgeVersion;
            }
            Files.write(path, List.of(
                    "modLoader=\"javafml\"",
                    "loaderVersion=\"[" + loaderVersion + ",)\"",
                    "license=\"" + license + "\"",
                    "issueTrackerURL=\"\"",
                    "",
                    "[[mods]]",
                    "modId=\"" + modid + "\"",
                    "version=\"${file.jarVersion}\"",
                    "displayName=\"" + name + "\"",
                    "displayURL=\"example.invalid\"",
                    "updateJSONURL=\"\"",
                    "authors=\"\"",
                    "description=\"\"\"",
                    "\"\"\"",
                    "",
                    "[[dependencies." + modid + "]]",
                    "    modId=\"forge\"",
                    "    mandatory=true",
                    "    versionRange=\"[" + requiredForgeVersion + ",)\"",
                    "    ordering=\"NONE\"",
                    "    side=\"BOTH\"",
                    "",
                    "[[dependencies." + modid + "]]",
                    "    modId=\"minecraft\"",
                    "    mandatory=true",
                    "    versionRange=\"[" + minecraftVersion + ",)\"",
                    "    ordering=\"NONE\"",
                    "    side=\"BOTH\"",
                    ""
            ), StandardOpenOption.CREATE);
        }
    }
    
    public static void createPackFile(Path path, String name, String minecraftVersion) throws IOException {
        if (!Files.exists(path)) {
            int resourceVersion = Versioning.getResourceVersion(minecraftVersion);
            JsonObject json = new JsonObject();
            JsonObject pack = new JsonObject();
            pack.addProperty("description", name + " resources");
            pack.addProperty("pack_format", resourceVersion);
            json.add("pack", pack);
            Files.writeString(path, ModGradle.GSON.toJson(json) + "\n", StandardOpenOption.CREATE);
        }
    }

    public static void createMixinFile(Path path, String modid, String group, String minecraftVersion) throws IOException {
        if (!Files.exists(path)) {
            MixinVersion version = Versioning.getMixinVersion(minecraftVersion);
            if (version == null) {
                throw new IllegalStateException("Can't create mixin config: No mixin available for minecraft " + minecraftVersion + ".");
            } else {
                String mixinVersion = version.release();
                Matcher m = MIN_MIXIN.matcher(mixinVersion);
                String minVersion = m.matches() ? m.group(1) : mixinVersion;
                JsonObject json = new JsonObject();
                json.addProperty("required", true);
                json.addProperty("compatibilityLevel", version.compatibility());
                json.addProperty("refmap", modid + ".refmap.json");
                json.addProperty("package", group + "." + modid + ".mixin");
                json.addProperty("minVersion", minVersion);
                json.add("client", new JsonArray());
                json.add("server", new JsonArray());
                json.add("mixins", new JsonArray());
                Files.writeString(path, ModGradle.GSON.toJson(json) + "\n", StandardOpenOption.CREATE);
            }
        }
    }
}
