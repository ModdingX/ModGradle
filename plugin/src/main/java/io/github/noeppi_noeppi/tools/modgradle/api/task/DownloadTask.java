package io.github.noeppi_noeppi.tools.modgradle.api.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Task to download a file from a {@link URL}.
 */
public abstract class DownloadTask extends DefaultTask {

    /**
     * The {@link URL} to download from.
     */
    @Input
    public abstract Property<URL> getUrl();

    /**
     * The output path to store the downloaded file.
     */
    @OutputFile
    public abstract RegularFileProperty getOutput();

    /**
     * Call this to make this task never up to date.
     */
    public void redownload() {
        this.getOutputs().upToDateWhen(task -> false);
    }
    
    @TaskAction
    protected void downloadResource(InputChanges inputs) throws IOException {
        Path path  = this.getOutput().get().getAsFile().toPath();
        if (!Files.isDirectory(path.getParent())) Files.createDirectories(path.getParent());
        URL url = this.getUrl().get();
        InputStream in = url.openStream();
        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        in.close();
    }
}
