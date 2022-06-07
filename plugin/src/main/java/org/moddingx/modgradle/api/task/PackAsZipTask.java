package org.moddingx.modgradle.api.task;

import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Task to pack a single file into a ZIP.
 */
public abstract class PackAsZipTask extends DefaultTask {

    /**
     * Gets the input file.
     */
    @InputFile
    public abstract RegularFileProperty getInput();

    /**
     * Gets the output ZIP file.
     */
    @OutputFile
    public abstract RegularFileProperty getOutput();

    /**
     * Gets the path inside the ZIP file.
     */
    @Input
    public abstract Property<String> getZipPath();

    @TaskAction
    protected void extractZip(InputChanges inputs) throws IOException {
        Path target = this.getOutput().getAsFile().get().toPath();
        PathUtils.createParentDirectories(target);
        ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
        out.putNextEntry(new ZipEntry(this.getZipPath().get()));
        Files.copy(this.getInput().getAsFile().get().toPath(), out);
        out.closeEntry();
        out.close();
    }
}
