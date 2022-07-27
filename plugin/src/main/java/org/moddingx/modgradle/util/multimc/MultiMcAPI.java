package org.moddingx.modgradle.util.multimc;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.moddingx.modgradle.ModGradle;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MultiMcAPI {
    
    public static final String ENDPOINT = "https://meta.multimc.org/v1";

    public static final String MC_UID = "net.minecraft";
    public static final String FORGE_UID = "net.minecraftforge";

    public static JsonObject buildForgePack(String version) throws IOException {
        return buildMmcPack(FORGE_UID, version);
    }

    public static JsonObject buildMmcPack(String uid, String version) throws IOException {
        Map<String, Component> components = new HashMap<>();
        Component main = resolve(uid, version);
        components.put(uid, main);
        addDependencies(components, main);

        JsonArray array = new JsonArray();
        Set<String> addedUids = new HashSet<>();
        // Stuff must be below its dependencies.
        // Minecraft is first
        if (components.containsKey(MC_UID)) addToList(components.get(MC_UID), components, uid, array, addedUids);
        // Primary uid is second
        addToList(main, components, uid, array, addedUids);
        // then everything else follows
        for (Component c : components.values()) {
            addToList(c, components, uid, array, addedUids);
        }
        JsonObject json = new JsonObject();
        json.addProperty("formatVersion", 1);
        json.add("components", array);
        return json;
    }

    private static void addToList(Component c, Map<String, Component> components, String mainUid, JsonArray array, Set<String> addedUids) {
        if (!addedUids.contains(c.uid())) {
            addedUids.add(c.uid());
            c.requires().stream()
                    .map(Dependency::uid)
                    .filter(components::containsKey)
                    .forEach(key -> addToList(components.get(key), components, mainUid, array, addedUids));
            boolean important = c.uid().equals(MC_UID);
            boolean dependency = !c.uid().equals(MC_UID) && !c.uid().equals(mainUid);
            array.add(c.toJson(important, dependency));
        }
    }

    private static void addDependencies(Map<String, Component> components, Component component) throws IOException {
        for (Dependency dep : component.requires()) {
            if (!components.containsKey(dep.uid())) {
                Component resolved = dep.resolve();
                components.put(resolved.uid(), resolved);
                addDependencies(components, resolved);
            }
        }
    }

    public static Component resolve(String uid, String version) throws IOException {
        JsonObject json = fetch("/" + uid + "/" + version + ".json");
        String name = json.has("name") ? json.get("name").getAsString() : uid;
        boolean isVolatile = json.has("volatile") && json.get("volatile").getAsBoolean();
        ImmutableList.Builder<Dependency> requires = ImmutableList.builder();
        if (json.has("requires")) {
            for (JsonElement elem : json.get("requires").getAsJsonArray()) {
                String dUid = elem.getAsJsonObject().get("uid").getAsString();
                String versionKey = "equals";
                if (!elem.getAsJsonObject().has("equals")) {
                    versionKey = "suggests";
                    if (!elem.getAsJsonObject().has("suggests")) {
                        throw new IllegalStateException("Failed to resolve require-version: " + elem);
                    }
                }
                String dVer = elem.getAsJsonObject().get(versionKey).getAsString();
                String dSuggest = elem.getAsJsonObject().has("suggests") ? elem.getAsJsonObject().get("suggests").getAsString() : null;
                requires.add(new Dependency(dUid, dVer, dSuggest));
            }
        }
        return new Component(uid, version, name, isVolatile, requires.build());
    }

    private static JsonObject fetch(String endpoint) throws IOException {
        URL url = new URL(ENDPOINT + endpoint);
        Reader reader = new InputStreamReader(url.openStream());
        JsonObject json = ModGradle.INTERNAL.fromJson(reader, JsonObject.class);
        reader.close();
        return json;
    }

    public record Component(String uid, String version, String name, boolean isVolatile, ImmutableList<Dependency> requires) {

        public JsonObject toJson(boolean important, boolean dependency) {
            JsonObject json = new JsonObject();
            json.addProperty("uid", this.uid);
            json.addProperty("version", this.version);
            json.addProperty("cachedName", this.name);
            json.addProperty("cachedVersion", this.version);
            if (this.isVolatile) json.addProperty("cachedVolatile", true);
            if (important) json.addProperty("important", true);
            if (dependency) json.addProperty("dependencyOnly", true);
            if (!this.requires.isEmpty()) {
                JsonArray array = new JsonArray();
                for (Dependency dep : this.requires) {
                    array.add(dep.toJson());
                }
                json.add("cachedRequires", array);
            }
            return json;
        }
    }

    public record Dependency(String uid, String version, @Nullable String suggest) {

        public Component resolve() throws IOException {
            return MultiMcAPI.resolve(this.uid, this.version);
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("uid", this.uid());
            json.addProperty("equals", this.version());
            if (this.suggest() != null) json.addProperty("suggests", this.suggest());
            return json;
        }
    }
}