package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.net.URL;

public class McUpdateData {

    @Nullable
    public final URL mappings;

    @Nullable
    public final URL transformer;

    public McUpdateData(JsonObject data) {
        try {
            this.mappings = data.has("mappings") ? new URL(data.get("mappings").getAsString()) : null;
            this.transformer = data.has("transformer") ? new URL(data.get("transformer").getAsString()) : null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mcupdate data.", e); 
        }
    }
}
