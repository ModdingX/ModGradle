package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackSettings;
import io.github.noeppi_noeppi.tools.modgradle.util.IOUtil;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.internal.provider.DefaultProvider;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class BuildModrinthPackTask extends BuildTargetTask {

    @Inject
    public BuildModrinthPackTask(PackSettings settings, List<CurseFile> files, String edition) {
        super(settings, files, edition);
        this.getArchiveExtension().convention(new DefaultProvider<>(() -> "mrpack"));
    }

    @Override
    protected void generate(Path target) throws IOException {
        FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + target.toUri()), Map.of(
                "create", String.valueOf(!Files.exists(target))
        ));
        Files.createDirectories(fs.getPath("overrides"));
        for (Path src : this.getOverridePaths(null)) {
            PathUtils.copyDirectory(src, fs.getPath("overrides"));
        }
        this.generateIndex(fs.getPath("modrinth.index.json"));
        fs.close();
    }

    private void generateIndex(Path target) throws IOException {
        // First compute hashes async
        ScheduledExecutorService service = new ScheduledThreadPoolExecutor(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
        Map<CurseFile, Map<String, String>> hashes = Collections.synchronizedMap(new HashMap<>());
        List<Future<?>> futures = new ArrayList<>();
        for (CurseFile file : this.files) {
            futures.add(service.submit(() -> {
                try {
                    Map<String, String> map = IOUtil.commonHashes(file.downloadUrl().toURL().openStream());
                    hashes.put(file, map);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        String capitalizedEdition = this.edition == null ? null : this.edition.substring(0, 1).toUpperCase(Locale.ROOT) + this.edition.substring(1);
        String editionPart = capitalizedEdition == null ? "" : " (" + capitalizedEdition + ")";

        JsonObject json = new JsonObject();

        json.addProperty("formatVersion", 1);
        json.addProperty("game", "minecraft");
        json.addProperty("name", this.getProject().getName() + editionPart);
        json.addProperty("versionId", this.getProject().getVersion().toString());

        JsonObject dependencies = new JsonObject();
        dependencies.addProperty("minecraft", this.settings.minecraft());
        dependencies.addProperty("forge", this.settings.forge());
        json.add("dependencies", dependencies);

        JsonArray fileArray = new JsonArray();
        for (CurseFile file : this.files.stream().sorted(Comparator.comparing(CurseFile::projectId)).toList()) {
            URI url = file.downloadUrl();
            JsonObject fileObj = new JsonObject();
            fileObj.addProperty("path", "mods/" + file.fileName());

            JsonArray downloadArray = new JsonArray();
            downloadArray.add(url.toString());
            fileObj.add("downloads", downloadArray);

            JsonObject env = new JsonObject();
            env.addProperty("client", file.side().client ? "required" : "unsupported");
            env.addProperty("server", file.side().server ? "required" : "unsupported");
            fileObj.add("env", env);

            JsonObject hashesObj = new JsonObject();
            if (hashes.containsKey(file)) {
                for (Map.Entry<String, String> entry : hashes.get(file).entrySet()) {
                    hashesObj.addProperty(entry.getKey(), entry.getValue());
                }
            }
            fileObj.add("hashes", hashesObj);
            fileObj.addProperty("fileSize", file.fileSize());

            fileArray.add(fileObj);
        }
        json.add("files", fileArray);

        Writer writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE_NEW);
        writer.write(ModGradle.GSON.toJson(json) + "\n");
        writer.close();
    }
}
