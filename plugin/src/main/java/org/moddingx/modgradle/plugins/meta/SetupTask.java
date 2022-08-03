package org.moddingx.modgradle.plugins.meta;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.api.Versioning;
import org.moddingx.modgradle.util.IOUtil;
import org.moddingx.modgradle.util.McEnv;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.stream.Stream;

public abstract class SetupTask extends DefaultTask {

    public SetupTask() {
        this.getMinecraftVersion().convention(McEnv.findMinecraftVersion(this.getProject()));
        this.getForgeVersion().convention(McEnv.findForgeVersion(this.getProject()));
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
    public abstract Property<String> getMinecraftVersion();

    @Input
    public abstract Property<String> getForgeVersion();

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
        Path clone = Files.createTempDirectory("modgradle-meta-setup");
        ProcessBuilder process = new ProcessBuilder().inheritIO().command("git", "clone", "-b", this.getRepoBranch().get(), this.getRepo().get().toString(), clone.toAbsolutePath().normalize().toString());
        process.redirectError(ProcessBuilder.Redirect.INHERIT);
        int exitCode;
        try {
            exitCode = process.start().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Unexpected interrupt", e);
        }
        if (exitCode != 0) {
            throw new IllegalStateException("Failed to clone repository: " + this.getRepo());
        }

        String modId = this.getModid().get();
        String minecraftVersion = this.getMinecraftVersion().get();
        String forgeVersion = this.getForgeVersion().get();
        String loaderVersion = forgeVersion.contains(".") ? forgeVersion.substring(0, forgeVersion.indexOf('.')) : forgeVersion;
        
        ImmutableMap.Builder<String, String> replaceBuilder = ImmutableMap.builder();
        replaceBuilder.put("name", this.getProject().getName());
        replaceBuilder.put("modid", modId);
        replaceBuilder.put("minecraft", minecraftVersion);
        replaceBuilder.put("forge", forgeVersion);
        replaceBuilder.put("fml", loaderVersion);
        replaceBuilder.put("license", this.getLicense().get());
        replaceBuilder.put("jdk", Integer.toString(Versioning.getJavaVersion(minecraftVersion)));
        replaceBuilder.put("resource", Integer.toString(Versioning.getResourceVersion(minecraftVersion)));
        Versioning.getDataVersion(minecraftVersion).ifPresent(v -> replaceBuilder.put("data", Integer.toString(v)));
        Map<String, String> replaces = replaceBuilder.build();
        
        this.copyDir(clone, "runClient");
        this.copyDir(clone, "runServer");
        this.copyDir(clone, "runData");
        this.copyDir(clone, "mod.github", ".github", replaces);

        this.copyFile(clone, "mod.gitignore", ".gitignore");
        this.copyFile(clone, "Jenkinsfile", replaces);
        

        Files.createDirectories(this.getProject().file("src/main/java").toPath()
                .resolve(this.getProject().getGroup().toString().replace('.', '/'))
                .resolve(modId));
        Files.createDirectories(this.getProject().file("src/main/resources/META-INF").toPath());
        Files.createDirectories(this.getProject().file("src/main/resources/assets").toPath()
                .resolve(modId).resolve("lang"));
        Files.createDirectories(this.getProject().file("src/main/resources/data").toPath()
                .resolve(modId));
        Files.createDirectories(this.getProject().file("src/generated/resources").toPath());

        if (!Files.exists(this.getProject().file("src/main/resources/META-INF/accesstransformer.cfg").toPath())) {
            Files.createFile(this.getProject().file("src/main/resources/META-INF/accesstransformer.cfg").toPath());
        }

        ModFiles.createToml(
                this.getProject().file("src/main/resources/META-INF/mods.toml").toPath(),
                modId, this.getProject().getName(), minecraftVersion,
                forgeVersion, this.getLicense().get()
        );
        ModFiles.createPackFile(
                this.getProject().file("src/main/resources/pack.mcmeta").toPath(),
                this.getProject().getName(), minecraftVersion
        );
        if (this.getMixin().get()) {
            ModFiles.createMixinFile(
                    this.getProject().file("src/main/resources/" + modId + ".mixins.json").toPath(),
                    modId, this.getProject().getGroup().toString(), minecraftVersion
            );
        }

        if (!Files.exists(this.getProject().file("LICENSE").toPath())) {
            InputStream in = this.getLicenseUrl().get().openStream();
            Files.copy(in, this.getProject().file("LICENSE").toPath());
            in.close();
        }
    }

    private void copyFile(Path clone, String file) throws IOException {
        this.copyFile(clone, file, file);
    }

    private void copyFile(Path clone, String fromFile, String toFile) throws IOException {
        if (Files.isRegularFile(clone.resolve(fromFile)) && !Files.exists(this.getProject().file(toFile).toPath())) {
            Files.copy(clone.resolve(fromFile), this.getProject().file(toFile).toPath());
        }
    }

    private void copyFile(Path clone, @SuppressWarnings("SameParameterValue") String file, Map<String, String> replace) throws IOException {
        this.copyFile(clone, file, file, replace);
    }

    private void copyFile(Path clone, String fromFile, String toFile, Map<String, String> replace) throws IOException {
        IOUtil.copyFile(clone.resolve(fromFile), this.getProject().file(toFile).toPath(), replace, false);
    }

    private void copyDir(Path clone, String dir) throws IOException {
        this.copyDir(clone, dir, dir);
    }

    private void copyDir(Path clone, String fromDir, String toDir) throws IOException {
        if (!Files.exists(this.getProject().file(toDir).toPath()) || this.isDirectoryEmpty(this.getProject().file(toDir).toPath())) {
            Files.createDirectories(this.getProject().file(toDir).toPath());
            if (Files.isDirectory(clone.resolve(fromDir))) {
                PathUtils.copyDirectory(clone.resolve(fromDir), this.getProject().file(toDir).toPath());
            }
        }
    }

    private void copyDir(Path clone, String dir, Map<String, String> replace) throws IOException {
        this.copyDir(clone, dir, dir, replace);
    }

    private void copyDir(Path clone, String fromDir, String toDir, Map<String, String> replace) throws IOException {
        if (!Files.exists(this.getProject().file(toDir).toPath()) || this.isDirectoryEmpty(this.getProject().file(toDir).toPath())) {
            Files.createDirectories(this.getProject().file(toDir).toPath());
            Files.walkFileTree(clone.resolve(fromDir), new FileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(SetupTask.this.getProject().file(toDir).toPath().toAbsolutePath().resolve(clone.resolve(fromDir).toAbsolutePath().relativize(dir.toAbsolutePath())));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path target = SetupTask.this.getProject().file(toDir).toPath().toAbsolutePath().resolve(clone.resolve(fromDir).toAbsolutePath().relativize(file.toAbsolutePath()));
                    IOUtil.copyFile(file, target, replace, false);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    throw new IOException("Failed to copy file: " + file.toAbsolutePath().normalize());
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    
    private boolean isDirectoryEmpty(Path path) throws IOException {
        if (!Files.isDirectory(path.toAbsolutePath().normalize())) return false;
        try (Stream<Path> paths = Files.list(path.toAbsolutePath().normalize())) {
            return paths.findAny().isEmpty();
        }
    }
}
