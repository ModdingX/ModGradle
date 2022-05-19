package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.util.CurseUtil;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public record CurseFile(int projectId, int fileId, Side side) {

    public URI downloadUrl() throws IOException {
        return CurseUtil.curseMaven("/curse/maven/O-" + this.projectId + "/" + this.fileId + "/O-" + this.projectId + "-" + this.fileId + ".jar");
    }

    public String fileName() throws IOException {
        return CurseUtil.API.getFile(this.projectId, this.fileId).name();
    }

    public static CurseFile parse(JsonObject json) {
        int projectId = json.get("project").getAsInt();
        int fileId = json.get("file").getAsInt();
        Side side = Side.byId(json.get("side").getAsString());
        return new CurseFile(projectId, fileId, side);
    }

    public static List<CurseFile> parseFiles(JsonElement json) {
        ImmutableList.Builder<CurseFile> list = ImmutableList.builder();
        for (JsonElement element : json.getAsJsonArray()) {
            list.add(CurseFile.parse(element.getAsJsonObject()));
        }

        return list.build();
    }

    public static Map<Integer, Integer> readCache(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            BufferedReader reader = Files.newBufferedReader(path);
            JsonArray array = ModGradle.INTERNAL.fromJson(reader, JsonArray.class);
            reader.close();
            ImmutableMap.Builder<Integer, Integer> map = ImmutableMap.builder();

            for (JsonElement element : array) {
                map.put(
                        element.getAsJsonObject().get("project").getAsInt(),
                        element.getAsJsonObject().get("file").getAsInt()
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
            JsonObject json = new JsonObject();
            json.addProperty("project", file.projectId);
            json.addProperty("file", file.fileId);
            array.add(json);
        }

        BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        writer.write(ModGradle.INTERNAL.toJson(array) + "\n");
        writer.close();
    }
}
