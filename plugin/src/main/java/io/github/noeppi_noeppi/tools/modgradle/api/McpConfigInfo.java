package io.github.noeppi_noeppi.tools.modgradle.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Used to read information for specific MCPConfig versions.
 */
@SuppressWarnings("ClassCanBeRecord")
public class McpConfigInfo {
    
    private static final Map<String, McpConfigInfo> CACHE = new HashMap<>();

    /**
     * The major java release, that is targeted.
     */
    public final int javaTarget;

    /**
     * Whether official class names should be used for this version of MCPConfig
     */
    public final boolean official;
    private final Map<String, List<String>> libraries;

    private McpConfigInfo(int javaTarget, boolean official, Map<String, List<String>> libraries) {
        this.javaTarget = javaTarget;
        this.official = official;
        this.libraries = libraries;
    }

    /**
     * Gets the additional libraries for a given pipeline.
     */
    public List<String> getLibraries(String pipeline) {
        if (this.libraries.containsKey(pipeline)) {
            return this.libraries.get(pipeline);
        } else {
            throw new IllegalStateException("Unknown MCPConfig pipeline: " + pipeline);
        }
    }

    /**
     * Gets the {@link McpConfigInfo} for a given MCPConfig version.
     */
    public static McpConfigInfo getInfo(String mcpConfig) throws IOException {
        if (CACHE.containsKey(mcpConfig)) return CACHE.get(mcpConfig);
        String mcv = mcpConfig.contains("-") ? mcpConfig.substring(0, mcpConfig.indexOf('-')) : mcpConfig;
        URL url = new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/" + mcpConfig + "/mcp_config-" + mcpConfig + ".zip");
        ZipInputStream zin = new ZipInputStream(url.openStream());
        for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
            String name = entry.getName();
            if (name.startsWith("/")) name = name.substring(1);
            if ("config.json".equals(name)) {
                InputStreamReader reader = new InputStreamReader(zin);
                JsonObject data = ModGradle.GSON.fromJson(reader, JsonObject.class);
                reader.close();
                zin.close();
                int javaTarget = data.has("java_target") ? data.get("java_target").getAsInt() : Versioning.getJavaVersion(mcv);
                boolean official = data.has("official") && data.get("official").getAsBoolean();
                ImmutableMap.Builder<String, List<String>> libraries = ImmutableMap.builder();
                for (String key : data.getAsJsonObject("libraries").keySet()) {
                    ImmutableList.Builder<String> list = ImmutableList.builder();
                    for (JsonElement library : data.getAsJsonObject("libraries").getAsJsonArray(key)) {
                        list.add(library.getAsString());
                    }
                    libraries.put(key, list.build());
                }
                McpConfigInfo info = new McpConfigInfo(javaTarget, official, libraries.build());
                CACHE.put(mcpConfig, info);
                return info;
            }
        }
        zin.close();
        throw new IllegalStateException("No mcp configuration found.");
    }
}
