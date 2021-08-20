package io.github.noeppi_noeppi.tools.modgradle.plugins.meta;

import io.github.noeppi_noeppi.tools.modgradle.api.Versioning;
import io.github.noeppi_noeppi.tools.modgradle.util.CopyUtil;
import io.github.noeppi_noeppi.tools.modgradle.util.McEnv;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class SetupTask extends DefaultTask {

    public final Property<String> modid = this.getProject().getObjects().property(String.class);
    public final Property<String> minecraftVersion = this.getProject().getObjects().property(String.class);
    public final Property<String> forgeVersion = this.getProject().getObjects().property(String.class);
    public final Property<String> license = this.getProject().getObjects().property(String.class);
    public final Property<URL> licenseUrl = this.getProject().getObjects().property(URL.class);
    public final Property<Boolean> mixin = this.getProject().getObjects().property(Boolean.class);
    public final Property<URL> repo = this.getProject().getObjects().property(URL.class);
    public final Property<String> repoBranch = this.getProject().getObjects().property(String.class);

    public SetupTask() {
        this.minecraftVersion.convention(new DefaultProvider<>(() -> McEnv.findMinecraftVersion(this.getProject())));
        this.forgeVersion.convention(new DefaultProvider<>(() -> McEnv.findForgeVersion(this.getProject())));
        this.license.convention("The Apache License, Version 2.0");
        this.mixin.convention(false);
        try {
            this.licenseUrl.convention(new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to configure conventions for SetupTask", e);
        }
        this.repoBranch.convention("master");
        this.getOutputs().upToDateWhen(t -> false);
    }

    @Input
    public String getModid() {
        return this.modid.get();
    }

    public void setModid(String modid) {
        this.modid.set(modid);
    }

    @Input
    public String getMinecraftVersion() {
        return this.minecraftVersion.get();
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion.set(minecraftVersion);
    }

    @Input
    public String getForgeVersion() {
        return this.forgeVersion.get();
    }

    public void setForgeVersion(String minecraftVersion) {
        this.forgeVersion.set(minecraftVersion);
    }

    @Input
    public String getLicense() {
        return this.license.get();
    }

    public void setLicense(String license) {
        this.license.set(license);
    }

    @Input
    public URL getLicenseUrl() {
        return this.licenseUrl.get();
    }

    public void setLicenseUrl(URL licenseUrl) {
        this.licenseUrl.set(licenseUrl);
    }

    @Input
    public boolean isMixin() {
        return this.mixin.get();
    }

    public void setMixin(boolean mixin) {
        this.mixin.set(mixin);
    }

    @Input
    public URL getRepo() {
        return this.repo.get();
    }

    public void setRepo(URL repo) {
        this.repo.set(repo);
    }

    @Input
    public String getRepoBranch() {
        return this.repoBranch.get();
    }

    public void setRepoBranch(String repoBranch) {
        this.repoBranch.set(repoBranch);
    }

    @TaskAction
    protected void runSetup(InputChanges inputs) throws IOException {
        Path clone = Files.createTempDirectory("modgradle-meta-setup");
        ProcessBuilder process = new ProcessBuilder().inheritIO()
                .command("git", "clone", "-b", this.getRepoBranch(), this.getRepo().toString(), clone.toAbsolutePath().normalize().toString());
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

        Map<String, String> replaces = Map.of(
                "name", this.getProject().getName(),
                "modid", this.getModid(),
                "minecraft", this.getMinecraftVersion(),
                "forge", this.getForgeVersion(),
                "license", this.getLicense(),
                "jdk", Integer.toString(Versioning.getJavaVersion(this.getMinecraftVersion())),
                "resource", Integer.toString(Versioning.getResourceVersion(this.getMinecraftVersion()))
        );
        
        this.copyDir(clone, "runClient");
        this.copyDir(clone, "runServer");
        this.copyDir(clone, "runData");
        this.copyDir(clone, "mod.github", ".github", replaces);

        this.copyFile(clone, "mod.gitignore", ".gitignore");
        this.copyFile(clone, "Jenkinsfile", replaces);
        

        Files.createDirectories(this.getProject().file("src/main/java").toPath()
                .resolve(this.getProject().getGroup().toString().replace('.', '/'))
                .resolve(this.getModid()));
        Files.createDirectories(this.getProject().file("src/main/resources/META-INF").toPath());
        Files.createDirectories(this.getProject().file("src/main/resources/assets").toPath()
                .resolve(this.getModid()).resolve("lang"));
        Files.createDirectories(this.getProject().file("src/main/resources/data").toPath()
                .resolve(this.getModid()));
        Files.createDirectories(this.getProject().file("src/generated/resources").toPath());

        if (!Files.exists(this.getProject().file("src/main/resources/META-INF/accesstransformer.cfg").toPath())) {
            Files.createFile(this.getProject().file("src/main/resources/META-INF/accesstransformer.cfg").toPath());
        }

        ModFiles.createToml(
                this.getProject().file("src/main/resources/META-INF/mods.toml").toPath(),
                this.getModid(), this.getProject().getName(), this.getMinecraftVersion(),
                this.getForgeVersion(), this.getLicense()
        );
        ModFiles.createPackFile(
                this.getProject().file("src/main/resources/pack.mcmeta").toPath(),
                this.getProject().getName(), this.getMinecraftVersion()
        );
        if (this.isMixin()) {
            ModFiles.createMixinFile(
                    this.getProject().file("src/main/resources/" + this.getModid() + ".mixins.json").toPath(),
                    this.getModid(), this.getProject().getGroup().toString(), this.getMinecraftVersion()
            );
        }

        if (!Files.exists(this.getProject().file("LICENSE").toPath())) {
            InputStream in = this.getLicenseUrl().openStream();
            Files.copy(in, this.getProject().file("LICENSE").toPath());
            in.close();
        }
    }

    private void copyFile(Path clone, @SuppressWarnings("SameParameterValue") String file) throws IOException {
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
        CopyUtil.copyFile(clone.resolve(fromFile), this.getProject().file(toFile).toPath(), replace, false);
    }

    private void copyDir(Path clone, String dir) throws IOException {
        this.copyDir(clone, dir, dir);
    }

    private void copyDir(Path clone, String fromDir, String toDir) throws IOException {
        if (!Files.exists(this.getProject().file(toDir).toPath())
                || Files.list(this.getProject().file(toDir).toPath()).findAny().isEmpty()) {
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
        if (!Files.exists(this.getProject().file(toDir).toPath())
                || Files.list(this.getProject().file(toDir).toPath()).findAny().isEmpty()) {
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
                    CopyUtil.copyFile(file, target, replace, false);
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
}
