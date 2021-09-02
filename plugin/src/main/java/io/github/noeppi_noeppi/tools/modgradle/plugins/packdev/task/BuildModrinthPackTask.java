package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackSettings;
import io.github.noeppi_noeppi.tools.modgradle.util.IOUtil;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;
import org.apache.commons.io.file.PathUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

public class BuildModrinthPackTask extends BuildTargetTask {

    @Inject
    public BuildModrinthPackTask(PackSettings settings, List<CurseFile> files) {
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
        this.generateIndex(fs.getPath("index.json"));
        fs.close();
    }
    
    private void generateIndex(Path target) throws IOException {
        JsonObject json = new JsonObject();
        
        json.addProperty("formatVersion", 1);
        json.addProperty("game", "minecraft");
        json.addProperty("name", this.getProject().getName());
        json.addProperty("versionId", this.getProject().getVersion().toString());

        JsonObject dependencies = new JsonObject();
        dependencies.addProperty("minecraft", this.settings.minecraft());
        dependencies.addProperty("forge", this.settings.forge());
        json.add("dependencies", dependencies);
        
        JsonArray fileArray = new JsonArray();
        for (CurseFile file : this.files) {
            URL url = file.downloadUrl();
            JsonObject fileObj = new JsonObject();
            fileObj.addProperty("path", "mods/" + file.fileName());
            
            JsonArray downloadArray = new JsonArray();
            downloadArray.add(url.toString());
            fileObj.add("downloads", downloadArray);
            
            JsonObject env = new JsonObject();
            env.addProperty("client", file.side().client ? "required" : "unsupported");
            env.addProperty("server", file.side().server ? "required" : "unsupported");
            fileObj.add("env", env);
            
            JsonObject hashes = new JsonObject();
            for (Map.Entry<String, String> entry : IOUtil.commonHashes(url.openStream()).entrySet()) {
                hashes.addProperty(entry.getKey(), entry.getValue());
            }
            fileObj.add("hashes", hashes);
            
            fileArray.add(fileObj);
        }
        json.add("files", fileArray);

        Writer writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE_NEW);
        writer.write(ModGradle.GSON.toJson(json) + "\n");
        writer.close();
    }
}
