package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

public record CurseFile(int projectId, int fileId, Side side) {
    
    public URL downloadUrl() throws IOException {
        URL urlQuery = new URL("https://addons-ecs.forgesvc.net/api/v2/addon/" + this.projectId + "/file/" + this.fileId + "/download-url");
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlQuery.openStream()));
        String downloadUrl = reader.readLine();
        reader.close();
        return new URL(downloadUrl);
    }
    
    public static CurseFile parse(JsonObject json) {
        int projectId = json.get("project").getAsInt();
        int fileId = json.get("file").getAsInt();
        Side side = Side.byId(json.get("side").getAsString());
        return new CurseFile(projectId, fileId, side);
    }
    
    public static List<CurseFile> parseFiles(JsonElement json) {
        ImmutableList.Builder<CurseFile> list = ImmutableList.builder();
        for (JsonElement elem : json.getAsJsonArray()) {
            list.add(parse(elem.getAsJsonObject()));
        }
        return list.build();
    }
}
