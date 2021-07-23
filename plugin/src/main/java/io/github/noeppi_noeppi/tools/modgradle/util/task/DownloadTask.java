package io.github.noeppi_noeppi.tools.modgradle.util.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
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

public class DownloadTask extends DefaultTask {

    private final Property<URL> url = this.getProject().getObjects().property(URL.class);
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();

    @Input
    public URL getURL() {
        return this.url.get();
    }

    public void setURL(URL url) {
        this.url.set(url);
    }

    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }

    @TaskAction
    protected void downloadResource(InputChanges inputs) throws IOException {
        Path path  = this.getOutput().getAsFile().toPath();
        if (!Files.isDirectory(path.getParent())) Files.createDirectories(path.getParent());
        URL url = this.getURL();
        InputStream in = url.openStream();
        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        in.close();
    }
    
    public void redownload() {
        this.getOutputs().upToDateWhen(task -> false);
    }
}
