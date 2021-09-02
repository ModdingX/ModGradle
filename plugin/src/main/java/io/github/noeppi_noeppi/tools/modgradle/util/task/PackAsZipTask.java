package io.github.noeppi_noeppi.tools.modgradle.util.task;

import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackAsZipTask extends DefaultTask {

    private final RegularFileProperty input = this.getProject().getObjects().fileProperty();
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();
    private final Property<String> path = this.getProject().getObjects().property(String.class);

    @InputFile
    public RegularFile getInput() {
        return this.input.get();
    }

    public void setInput(RegularFile input) {
        this.input.set(input);
    }

    public void setInput(File input) {
        this.input.set(input);
    }

    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }

    public void setOutput(File output) {
        this.output.set(output);
    }

    @Input
    public String getZipPath() {
        return this.path.get();
    }

    public void setZipPath(String zipPath) {
        this.path.set(zipPath);
    }

    @TaskAction
    protected void extractZip(InputChanges inputs) throws IOException {
        Path target = this.getOutput().getAsFile().toPath();
        if (!Files.exists(target.getParent())) {
            PathUtils.createParentDirectories(target.getParent());
        }
        ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
        zout.putNextEntry(new ZipEntry(this.getZipPath()));
        Files.copy(this.getInput().getAsFile().toPath(), zout);
        zout.closeEntry();
        zout.close();
    }
}
