package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackSettings;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;
import org.apache.commons.io.file.PathUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BuildCursePackTask extends BuildTargetTask {

    @Inject
    public BuildCursePackTask(PackSettings settings, List<CurseFile> files) {
        super(settings, files);
    }

    @Override
    protected void generate(Path target) throws IOException {
        FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + target.toUri()), Map.of(
                "create", String.valueOf(!Files.exists(target))
        ));
        Files.createDirectories(fs.getPath("overrides"));
        if (Files.exists(this.getProject().file("data/" + Side.COMMON.id).toPath())) PathUtils.copyDirectory(this.getProject().file("data/" + Side.COMMON.id).toPath(), fs.getPath("overrides"));
        if (Files.exists(this.getProject().file("data/" + Side.CLIENT.id).toPath())) PathUtils.copyDirectory(this.getProject().file("data/" + Side.CLIENT.id).toPath(), fs.getPath("overrides"));
        this.generateManifest(fs.getPath("manifest.json"));
        this.generateModList(fs.getPath("modlist.html"));
        fs.close();
    }
    
    private void generateManifest(Path target) throws IOException {
        JsonObject json = new JsonObject();
        
        JsonObject minecraftBlock = new JsonObject();
        minecraftBlock.addProperty("version", this.settings.minecraft());

        JsonArray modLoaders = new JsonArray();
        JsonObject modLoader = new JsonObject();
        modLoader.addProperty("id", "forge-" + this.settings.forge());
        modLoader.addProperty("primary", true);
        modLoaders.add(modLoader);
        
        minecraftBlock.add("modLoaders", modLoaders);
        json.add("minecraft", minecraftBlock);
        
        json.addProperty("manifestType", "minecraftModpack");
        json.addProperty("overrides", "overrides");
        json.addProperty("manifestVersion", 1);
        json.addProperty("version", this.getProject().getVersion().toString());
        this.settings.author().ifPresent(author -> json.addProperty("author", author));
        json.addProperty("projectID", this.settings.projectId());
        json.addProperty("name", this.getProject().getName());
        
        JsonArray fileArray = new JsonArray();
        for (CurseFile file : this.files.stream().sorted(Comparator.comparing(CurseFile::projectId)).toList()) {
            if (file.side().client) {
                JsonObject fileObj = new JsonObject();
                fileObj.addProperty("projectID", file.projectId());
                fileObj.addProperty("fileID", file.fileId());
                fileArray.add(fileObj);
            }
        }
        json.add("files", fileArray);

        Writer writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE_NEW);
        writer.write(ModGradle.GSON.toJson(json) + "\n");
        writer.close();
    }
    
    private void generateModList(Path target) throws IOException {
        Map<String, String> linesBySlug = new HashMap<>();
        for (CurseFile file : this.files) {
            URL url = new URL("https://addons-ecs.forgesvc.net/api/v2/addon/" + file.projectId());
            Reader reader = new InputStreamReader(url.openStream());
            JsonObject data = ModGradle.INTERNAL.fromJson(reader, JsonObject.class);
            
            String name = data.get("name").getAsString();
            String slug = data.get("slug").getAsString();
            String author = "";
            if (data.has("authors") && !data.getAsJsonArray("authors").isEmpty()) {
                author = Streams.stream(data.getAsJsonArray("authors"))
                        .map(JsonElement::getAsJsonObject)
                        .map(a -> a.get("name").getAsString())
                        .map(a -> "<a href=\"https://www.curseforge.com/members/" + a + "/projects\">" + a + "</a>")
                        .collect(Collectors.joining(", ", " (by ", ")"));
            }
            linesBySlug.put(slug, "<li><a href=\"https://www.curseforge.com/projects/" + file.projectId() + "\">" + name + "</a>" + author + "</li>");
        }
        Writer writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE_NEW);
        writer.write("<h2>" + this.getProject().getName() + " - " + this.getProject().getVersion() + "</h2>\n");
        writer.write("\n");
        writer.write("<ul>\n");
        for (String line : linesBySlug.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList()) {
            writer.write(line + "\n");
        }
        writer.write("</ul>\n");
        writer.close();
    }
}
