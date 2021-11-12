package io.github.noeppi_noeppi.tools.modgradle.plugins.coremods;

import io.github.noeppi_noeppi.tools.modgradle.util.ProcessUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.*;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

public class BuildCoreModsTask extends DefaultTask {
    
    private final RegularFileProperty coreModTypes = this.getProject().getObjects().fileProperty();
    private final Property<FileCollection> coreModSources = this.getProject().getObjects().property(FileCollection.class);
    private final DirectoryProperty targetDir = this.getProject().getObjects().directoryProperty();
    
    public BuildCoreModsTask() {
        this.targetDir.set(this.getProject().file("build").toPath().resolve("coremods").toFile());
    }

    @InputFile
    public RegularFile getCoreModTypes() {
        return this.coreModTypes.get();
    }

    public void setCoreModTypes(RegularFile coreModTypes) {
        this.coreModTypes.set(coreModTypes);
    }

    public void setCoreModTypes(File coreModTypes) {
        this.coreModTypes.set(coreModTypes);
    }

    @InputFiles
    public FileCollection getCoreModSources() {
        return this.coreModSources.get();
    }

    public void setCoreModSources(FileCollection coreModSources) {
        this.coreModSources.set(coreModSources);
    }

    @OutputDirectory
    public Directory getTargetDir() {
        return this.targetDir.get();
    }
    
    public void setTargetDir(Directory targetDir) {
        this.targetDir.set(targetDir);
    }

    public void setTargetDir(File targetDir) {
        this.targetDir.set(targetDir);
    }

    @OutputDirectory
    public Directory getOutputDir() {
        return this.targetDir.get().dir("ts");
    }
    
    @TaskAction
    public void compileCoreMods(InputChanges changes) throws IOException {
        FileCollection sources = this.getCoreModSources();
        Path install = this.getTargetDir().getAsFile().toPath();
        Path target = this.getOutputDir().getAsFile().toPath();
        if (!Files.exists(target)) Files.createDirectories(target);
        
        for (File srcDirFile : sources.getFiles()) {
            Path srcDir = srcDirFile.toPath().toAbsolutePath().normalize();
            List<Path> coreMods = Files.walk(srcDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ts"))
                    .map(p -> srcDir.relativize(p.toAbsolutePath()))
                    .toList();
            for (Path loc : coreMods) {
                Path dest = target.resolve(loc);
                if (!Files.isDirectory(dest.getParent())) Files.createDirectories(dest.getParent());
                Files.copy(srcDir.resolve(loc), dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        
        try (FileSystem fs = FSUtil.getFileSystem(URI.create("jar:" + this.getCoreModTypes().getAsFile().toPath().toUri()))) {
            Files.copy(fs.getPath("coremods.d.ts"), target.resolve("coremods.d.ts"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(fs.getPath("tsconfig.json"), target.resolve("tsconfig.json"), StandardCopyOption.REPLACE_EXISTING);
        }

        ProcessUtil.run(install, "npm", "install", "typescript");
        ProcessUtil.run(target, "npx", "tsc");
    }
}
