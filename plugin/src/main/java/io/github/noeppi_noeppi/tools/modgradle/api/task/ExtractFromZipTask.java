package io.github.noeppi_noeppi.tools.modgradle.api.task;

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
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Task to extract a file from a ZIP.
 */
public abstract class ExtractFromZipTask extends DefaultTask {

    /**
     * The ZIP file to extract from.
     */
    @InputFile
    public abstract RegularFileProperty getInput();
    
    /**
     * Output path for the extracted file.
     */
    @OutputFile
    public abstract RegularFileProperty getOutput();

    /**
     * The path inside the ZIP file to extract. If it does not exist, the task will fail.
     */
    @Input
    public abstract Property<String> getZipPath();

    @TaskAction
    protected void extractZip(InputChanges inputs) throws IOException {
        Path target = this.getOutput().getAsFile().get().toPath();
        String zipPath = this.getZipPath().get();
        PathUtils.createParentDirectories(target);
        ZipInputStream zin = new ZipInputStream(Files.newInputStream(this.getInput().getAsFile().get().toPath()));
        for (ZipEntry zf = zin.getNextEntry(); zf != null; zf = zin.getNextEntry()) {
            String name = zf.getName();
            while (name.startsWith("/")) name = name.substring(1);
            if (name.equals(zipPath)) {
                Files.copy(zin, target, StandardCopyOption.REPLACE_EXISTING);
                zin.close();
                return;
            }
        }
        zin.close();
        throw new IllegalStateException(this.getZipPath() + " not found in zip file.");
    }
}
