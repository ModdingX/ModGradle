package org.moddingx.modgradle.plugins.meta;

import com.google.gson.JsonObject;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.util.McEnv;
import org.moddingx.modgradle.util.ModFiles;
import org.moddingx.modgradle.util.io.CopyHelper;
import org.moddingx.modgradle.util.io.TemplateCopyOption;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public abstract class SetupTask extends DefaultTask {

    public SetupTask() {
        this.getMixin().convention(false);
        this.getLicense().convention("The Apache License, Version 2.0");
        try {
            this.getLicenseUrl().convention(new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to configure conventions for SetupTask", e);
        }
        this.getRepoBranch().convention("master");
        this.getOutputs().upToDateWhen(t -> false);
    }

    @Input
    public abstract Property<String> getModid();

    @Input
    public abstract Property<String> getLicense();

    @Input
    public abstract Property<URL> getLicenseUrl();

    @Input
    public abstract Property<Boolean> getMixin();

    @Input
    public abstract Property<URL> getRepo();

    @Input
    public abstract Property<String> getRepoBranch();

    @TaskAction
    protected void runSetup(InputChanges inputs) throws IOException {
//        Path clone = Files.createTempDirectory("modgradle-meta-setup");
//        ProcessBuilder process = new ProcessBuilder().inheritIO().command("git", "clone", "-b", this.getRepoBranch().get(), this.getRepo().get().toString(), clone.toAbsolutePath().normalize().toString());
//        process.redirectError(ProcessBuilder.Redirect.INHERIT);
//        int exitCode;
//        try {
//            exitCode = process.start().waitFor();
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("Unexpected interrupt", e);
//        }
//        if (exitCode != 0) {
//            throw new IllegalStateException("Failed to clone repository: " + this.getRepo());
//        }
        Path clone = Paths.get("/home/tux/dev/util/ModUtils");

        Map<String, Object> replacements = Map.of(
                "modid", this.getModid().get(),
                "license", this.getLicense().get(),
                "mixin_enabled", this.getMixin().get()
        );

        Path template = clone.resolve("template");
        Path renames = clone.resolve("renames.txt");
        if (!Files.isDirectory(template)) throw new IllegalStateException("repositorys does not contain a template: File not found: " + template.toAbsolutePath());
        if (!Files.isRegularFile(renames)) throw new IllegalStateException("repositorys contains no renames: File not found: " + renames.toAbsolutePath());
        CopyHelper.copyTemplateDir(this.getProject(), template, this.getProject().file(".").toPath(), renames, replacements, TemplateCopyOption.SKIP_EXISTING);
        
        // Special case: Pack versions are handles by ModGradle itself
        // (we need them for mcupdate anyway, makes thing easier)
        if (Files.exists(this.getProject().file("src/main/resources/pack.mcmeta").toPath())) {
            String minecraft = McEnv.findMinecraftVersion(this.getProject()).get();
            Path path = this.getProject().file("src/main/resources/pack.mcmeta").toPath();
            JsonObject packJson;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                packJson = ModGradle.GSON.fromJson(reader, JsonObject.class);
            }
            ModFiles.addPackVersions(packJson, minecraft);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
                ModGradle.GSON.toJson(packJson, writer);
            }
        }
        
        if (!Files.exists(this.getProject().file("LICENSE").toPath())) {
            InputStream in = this.getLicenseUrl().get().openStream();
            Files.copy(in, this.getProject().file("LICENSE").toPath());
            in.close();
        }
    }
}
