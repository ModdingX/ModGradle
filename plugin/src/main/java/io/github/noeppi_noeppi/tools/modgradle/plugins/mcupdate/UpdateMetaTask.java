package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate;

import com.google.gson.JsonElement;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateMetaTask extends DefaultTask {

    private final Property<FileCollection> files = this.getProject().getObjects().property(FileCollection.class);
    
    public UpdateMetaTask() {
        this.files.convention(new DefaultProvider<>(() -> JavaEnv.getJavaResourceDirs(this.getProject())));
        this.getOutputs().upToDateWhen(t -> false);
    }
    
    @InputFiles
    public FileCollection getFiles() {
        return this.files.get();
    }

    public void setFiles(FileCollection files) {
        this.files.set(files);
    }
    
    @TaskAction
    protected void updateMeta(InputChanges inputs) throws IOException {
        Path jenkins = this.getProject().file("Jenkinsfile").toPath();
        if (Files.exists(jenkins)) {
            BufferedReader reader = Files.newBufferedReader(jenkins);
            List<String> lines = reader.lines().collect(Collectors.toList());
            reader.close();
            if (lines.stream().noneMatch(l -> l.contains("jdk 'java16'"))) {
                boolean hasWritten16 = false;
                BufferedWriter writer = Files.newBufferedWriter(jenkins, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                for (String line : lines) {
                    if (line.contains("stages") && !hasWritten16) {
                        StringBuilder indentBuilder = new StringBuilder();
                        for (int i = 0; i < line.length(); i++) {
                            if (!Character.isWhitespace(line.charAt(i))) break;
                            indentBuilder.append(line.charAt(i));
                        }
                        String indent = indentBuilder.toString();
                        writer.write(indent + "tools {\n");
                        writer.write(indent + "  jdk 'java16'\n");
                        writer.write(indent + "}\n");
                        hasWritten16 = true;
                    }
                    writer.write(line + "\n");
                }
                writer.close();
                System.out.println("Added java 16 jdk to " + jenkins.toAbsolutePath().normalize().toString());
            }
        }
        this.getFiles().forEach(file -> {
            try {
                this.processResourceDirectory(file.toPath());
            } catch (IOException e) {
                throw new RuntimeException("Failed to update metadata: " + file.toPath().toAbsolutePath().normalize().toString());
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
                json.getAsJsonObject().getAsJsonObject("pack").addProperty("pack_format", 7);
                Writer out = Files.newBufferedWriter(resourcePackPath, StandardOpenOption.TRUNCATE_EXISTING);
                ModGradle.GSON.toJson(json, out);
                out.write("\n");
                out.close();
                System.out.println("Set pack_version to 7 in " + resourcePackPath.toAbsolutePath().normalize().toString());
            }
        }
    }
}
