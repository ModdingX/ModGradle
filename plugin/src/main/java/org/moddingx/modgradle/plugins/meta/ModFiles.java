package org.moddingx.modgradle.plugins.meta;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.api.MixinVersion;
import org.moddingx.modgradle.api.Versioning;

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
                    "displayURL=\"\"",
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
            int formatVersion = Math.max(Versioning.getResourceVersion(minecraftVersion), Versioning.getDataVersion(minecraftVersion).orElse(0));
            JsonObject json = new JsonObject();
            JsonObject pack = new JsonObject();
            pack.addProperty("description", name + " resources");
            pack.addProperty("pack_format", formatVersion);
            Versioning.getDataVersion(minecraftVersion).stream().filter(v -> v >= 9).forEach(dataVersion -> {
                pack.addProperty("forge:resource_pack_format", Versioning.getResourceVersion(minecraftVersion));
                pack.addProperty("forge:data_pack_format", dataVersion);
            });
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
