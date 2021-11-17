package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate.task;

import com.google.gson.JsonElement;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.api.Versioning;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public abstract class UpdateMetaTask extends DefaultTask {
    
    public UpdateMetaTask() {
        this.getResources().convention(JavaEnv.getJavaResourceDirs(this.getProject()));
        this.getOutputs().upToDateWhen(t -> false);
    }
    
    @InputFiles
    public abstract Property<FileCollection> getResources();
    
    @Input
    public abstract Property<String> getMinecraft();
    
    @TaskAction
    protected void updateMeta(InputChanges inputs) throws IOException {
        this.getResources().get().getFiles().forEach(file -> {
            try {
                this.processResourceDirectory(file.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to update metadata: " + file.toPath().toAbsolutePath().normalize());
            }
        });
    }
    
    private void processResourceDirectory(Path path) throws IOException {
        Path resourcePackPath = path.resolve("pack.mcmeta");
        if (Files.exists(resourcePackPath)) {
            Reader in = Files.newBufferedReader(resourcePackPath);
            JsonElement json = ModGradle.GSON.fromJson(in, JsonElement.class);
            in.close();
            if (json.isJsonObject() && json.getAsJsonObject().has("pack")) {
                json.getAsJsonObject().getAsJsonObject("pack").addProperty("pack_format", Versioning.getResourceVersion(this.getMinecraft().get()));
                Writer out = Files.newBufferedWriter(resourcePackPath, StandardOpenOption.TRUNCATE_EXISTING);
                ModGradle.GSON.toJson(json, out);
                out.write("\n");
                out.close();
            }
        }
    }
}
