package io.github.noeppi_noeppi.tools.modgradle.util.task;

import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExtractZipTask extends DefaultTask {

    private final RegularFileProperty input = this.getProject().getObjects().fileProperty();
    private final DirectoryProperty output = this.getProject().getObjects().directoryProperty();
    
    @InputFile
    public RegularFile getInput() {
        return this.input.get();
    }

    public void setInput(RegularFile input) {
        this.input.set(input);
    }
    
    @OutputDirectory
    public Directory getOutput() {
        return this.output.get();
    }

    public void setOutput(Directory output) {
        this.output.set(output);
    }

    public void setOutput(File outputDir) {
        this.output.set(outputDir);
    }
    
    @TaskAction
    protected void extractZip(InputChanges inputs) throws IOException {
        Path targetPath = this.getOutput().getAsFile().toPath();
        if (Files.exists(targetPath)) {
            PathUtils.deleteDirectory(targetPath);
        }
        Files.createDirectories(targetPath);
        ZipInputStream zin = new ZipInputStream(Files.newInputStream(this.getInput().getAsFile().toPath()));
        for (ZipEntry zf = zin.getNextEntry(); zf != null; zf = zin.getNextEntry()) {
            String name = zf.getName();
            while (name.startsWith("/")) name = name.substring(1);
            Path target = targetPath.resolve(name);
            if (zf.isDirectory()) {
                Files.createDirectories(target);
            } else {
                Files.createDirectories(target.getParent());
                Files.copy(zin, target);
            }
        }
        zin.close();
    }
}
