package org.moddingx.modgradle.plugins.mcupdate;

import com.google.gson.JsonObject;
import org.moddingx.modgradle.ModGradle;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public record McUpdateData(
        @Nullable URL mappings,
        @Nullable URL transformer
) {

    public static McUpdateData load(URL url) throws IOException {
        try (Reader reader = new InputStreamReader(url.openStream())) {
            JsonObject json = ModGradle.GSON.fromJson(reader, JsonObject.class);
            
            URL mappings = json.has("mappings") ? loadURL(url, json.get("mappings").getAsString()) : null;
            URL transformer = json.has("transformer") ? loadURL(url, json.get("transformer").getAsString()) : null;
            
            return new McUpdateData(mappings, transformer);
        }
    }
    
    private static URL loadURL(URL baseUrl, String value) throws IOException {
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            try {
                return baseUrl.toURI().resolve(new URI(value)).toURL();
            } catch (IOException | URISyntaxException | IllegalArgumentException x) {
                e.addSuppressed(x);
                throw e;
            }
        }
    }
}
