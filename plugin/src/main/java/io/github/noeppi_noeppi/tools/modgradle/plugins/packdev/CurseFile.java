package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public record CurseFile(int projectId, int fileId, Side side) {
    
    public URL downloadUrl() throws IOException {
        return new URL("https://www.cursemaven.com/curse/maven/O-" + this.projectId + "/" + this.fileId + "/O-" + this.projectId + "-" + this.fileId + ".jar");
    }
    
    public String fileName() throws IOException {
        URL urlQuery = new URL("https://addons-ecs.forgesvc.net/api/v2/addon/" + this.projectId + "/file/" + this.fileId + "/download-url");
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlQuery.openStream()));
        String downloadUrl = reader.readLine().trim();
        reader.close();
        URL base = new URL(downloadUrl);
        return base.getFile().contains("/") ? base.getFile().substring(base.getFile().lastIndexOf('/') + 1) : base.getFile();
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
    
    public static Map<Integer, Integer> readCache(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            Reader reader = Files.newBufferedReader(path);
            JsonArray array = ModGradle.INTERNAL.fromJson(reader, JsonArray.class);
            reader.close();
            ImmutableMap.Builder<Integer, Integer> map = ImmutableMap.builder();
            for (JsonElement elem : array) {
                map.put(
                        elem.getAsJsonObject().get("project").getAsInt(),
                        elem.getAsJsonObject().get("file").getAsInt()
                );
            }
            return map.build();
        } else {
            return Map.of();
        }
    }
    
    public static void writeCache(Path path, List<CurseFile> files) throws IOException {
        JsonArray array = new JsonArray();
        for (CurseFile file : files) {
            JsonObject obj = new JsonObject();
            obj.addProperty("project", file.projectId());
            obj.addProperty("file", file.fileId());
            array.add(obj);
        }
        Writer writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        writer.write(ModGradle.INTERNAL.toJson(array) + "\n");
        writer.close();
    }
}
