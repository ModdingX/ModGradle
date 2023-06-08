package org.moddingx.modgradle.plugins.mcupdate.task;

import com.google.gson.JsonElement;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.api.Versioning;
import org.moddingx.modgradle.util.ModFiles;
import org.moddingx.modgradle.util.java.JavaEnv;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
        this.processFiles(this.getResources().get().getFiles().stream().map(File::toPath), this::processResourceDirectory);
        this.processFiles(this.findProjectRelative("Jenkinsfile"), this::processJenkinsfile);
    }
    
    private void processResourceDirectory(String minecraft, Path path) throws IOException {
        Path resourcePackPath = path.resolve("pack.mcmeta");
        if (Files.exists(resourcePackPath)) {
            Reader in = Files.newBufferedReader(resourcePackPath);
            JsonElement json = ModGradle.GSON.fromJson(in, JsonElement.class);
            in.close();
            if (json.isJsonObject() && json.getAsJsonObject().has("pack")) {
                ModFiles.addPackVersions(json.getAsJsonObject(), minecraft);
                Writer out = Files.newBufferedWriter(resourcePackPath, StandardOpenOption.TRUNCATE_EXISTING);
                ModGradle.GSON.toJson(json, out);
                out.write("\n");
                out.close();
            }
        }
    }
    
    private void processJenkinsfile(String minecraft, Path path) throws IOException {
        Pattern pattern = Pattern.compile("(tools[\\s\n]*\\{[.\\s]*?jdk\\s*['\"]java)\\d+(['\"][.\\s]*?})");
        String file = Files.readString(path);
        String replaced = pattern.matcher(file).replaceAll(r -> r.group(1) + Versioning.getJavaVersion(minecraft) + r.group(2));
        if (!file.equals(replaced)) {
            Files.writeString(path, replaced, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
    
    private void processFiles(Stream<Path> files, Action action) {
        String minecraft = this.getMinecraft().get();
        files.forEach(path -> {
            try {
                action.perform(minecraft, path.toAbsolutePath().normalize());
            } catch (IOException e) {
                throw new RuntimeException("Failed to apply mcupdate: Failed to process " + path.toAbsolutePath().normalize(), e);
            }
        });
    }
    
    private Stream<Path> findProjectRelative(@SuppressWarnings("SameParameterValue") String file) {
        Stream.Builder<Path> stream = Stream.builder();
        Path local = this.getProject().file(file).toPath().toAbsolutePath().normalize();
        if (Files.exists(local)) stream.add(local);
        Path root = this.getProject().getRootProject().file(file).toPath().toAbsolutePath().normalize();
        if (Files.exists(root)) stream.add(root);
        return stream.build();
    }
    
    @FunctionalInterface
    private interface Action {
        void perform(String minecraft, Path path) throws IOException;
    }
}
