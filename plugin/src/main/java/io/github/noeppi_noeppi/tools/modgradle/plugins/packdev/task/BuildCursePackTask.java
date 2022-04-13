package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.cursewrapper.api.response.ProjectInfo;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackSettings;
import io.github.noeppi_noeppi.tools.modgradle.util.CurseUtil;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;
import org.apache.commons.io.file.PathUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

public class BuildCursePackTask extends BuildTargetTask {

    @Inject
    public BuildCursePackTask(PackSettings settings, List<CurseFile> files, String edition) {
        super(settings, files, edition);
    }

    @Override
    protected void generate(Path target) throws IOException {
        FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + target.toUri()), Map.of(
                "create", String.valueOf(!Files.exists(target))
        ));
        Files.createDirectories(fs.getPath("overrides"));
        for (Path src : this.getOverridePaths(Side.CLIENT)) {
            PathUtils.copyDirectory(src, fs.getPath("overrides"));
        }

        this.generateManifest(fs.getPath("manifest.json"));
        this.generateModList(fs.getPath("modlist.html"));
        fs.close();
    }

    private void generateManifest(Path target) throws IOException {
        String capitalizedEdition = this.edition == null ? null : this.edition.substring(0, 1).toUpperCase(Locale.ROOT) + this.edition.substring(1);
        String editionPart = capitalizedEdition == null ? "" : " (" + capitalizedEdition + ")";

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
        json.addProperty("name", this.getProject().getName() + editionPart);

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
            ProjectInfo info = CurseUtil.API.getProject(file.projectId());

            String name = info.name();
            String slug = info.slug();
            String author = "<a href=\"https://www.curseforge.com/members/" + info.owner() + "/projects\">" + info.owner() + "</a>";
            linesBySlug.put(slug, "<li><a href=\"" + info.website() + "\">" + name + "</a> (" + author + ")</li>");
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
