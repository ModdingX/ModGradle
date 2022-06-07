package org.moddingx.modgradle.api.task;

import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Task to extract a zip file.
 */
public abstract class ExtractZipTask extends DefaultTask {

    /**
     * The ZIP file to extract.
     */
    @InputFile
    public abstract RegularFileProperty getInput();

    /**
     * The destination directory to extract to.
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutput();
    
    @TaskAction
    protected void extractZip(InputChanges inputs) throws IOException {
        Path targetPath = this.getOutput().getAsFile().get().toPath();
        if (Files.exists(targetPath)) {
            PathUtils.deleteDirectory(targetPath);
        }
        Files.createDirectories(targetPath);
        ZipInputStream zin = new ZipInputStream(Files.newInputStream(this.getInput().getAsFile().get().toPath()));
        for (ZipEntry zf = zin.getNextEntry(); zf != null; zf = zin.getNextEntry()) {
            String name = zf.getName();
            while (name.startsWith("/")) name = name.substring(1);
            Path target = targetPath.resolve(name);
            if (zf.isDirectory()) {
                Files.createDirectories(target);
            } else {
                PathUtils.createParentDirectories(target);
                Files.copy(zin, target);
            }
        }
        zin.close();
    }
}
