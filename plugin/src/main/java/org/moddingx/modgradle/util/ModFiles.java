package org.moddingx.modgradle.util;

import com.google.gson.JsonObject;
import org.moddingx.modgradle.api.Versioning;

public class ModFiles {
    
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
}
