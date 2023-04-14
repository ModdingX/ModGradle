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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

            Files.write(path, Stream.of(
                    "modLoader=\"javafml\"",
                    "loaderVersion=\"[" + loaderVersion + ",)\"",
                    "license=\"" + license + "\"",
                    "issueTrackerURL=\"\"",
                    "",
                    "[[mods]]",
                    "modId=\"" + modid + "\"",
                    "version=\"${file.jarVersion}\"",
                    "displayName=\"" + name + "\"",
                    Versioning.getDataVersion(minecraftVersion).stream().anyMatch(data -> data >= 10) ? "displayTest=\"MATCH_VERSION\"" : "\0",
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
            ).filter(line -> !"\0".equals(line)).toList(), StandardOpenOption.CREATE_NEW);
        }
    }

    public static void createPackFile(Path path, String name, String minecraftVersion) throws IOException {
        if (!Files.exists(path)) {
            JsonObject json = new JsonObject();
            JsonObject pack = new JsonObject();
            pack.addProperty("description", name + " resources");
            json.add("pack", pack);
            addPackVersions(json, minecraftVersion);
            Files.writeString(path, ModGradle.GSON.toJson(json) + "\n", StandardOpenOption.CREATE);
        }
    }
    
    public static void addPackVersions(JsonObject json, String minecraftVersion) {
        JsonObject pack = json.has("pack") && json.get("pack").isJsonObject() ? json.get("pack").getAsJsonObject() : new JsonObject();
        int resourceVersion = Versioning.getResourceVersion(minecraftVersion);
        int dataVersion = Versioning.getDataVersion(minecraftVersion).orElse(resourceVersion);
        pack.addProperty("pack_format", Math.max(resourceVersion, dataVersion));
        pack.remove("forge:client_resources_pack_format");
        pack.remove("forge:server_data_pack_format");
        pack.remove("forge:resource_pack_format");
        pack.remove("forge:data_pack_format");
        if (dataVersion >= 12) {
            pack.addProperty("forge:client_resources_pack_format", resourceVersion);
            pack.addProperty("forge:server_data_pack_format", dataVersion);
        } else if (dataVersion >= 9) {
            pack.addProperty("forge:resource_pack_format", resourceVersion);
            pack.addProperty("forge:data_pack_format", dataVersion);
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
