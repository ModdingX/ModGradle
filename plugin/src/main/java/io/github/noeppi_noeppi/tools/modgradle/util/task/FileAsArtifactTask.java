package io.github.noeppi_noeppi.tools.modgradle.util.task;

import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.work.InputChanges;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

// Wrap a file into a task that can be used for maven publish
public class FileAsArtifactTask extends AbstractArchiveTask {
    
    private final Property<RegularFile> file = this.getProject().getObjects().fileProperty();

    public FileAsArtifactTask() {
        this.getDestinationDirectory().set(this.getProject().file("build").toPath().resolve(this.getName()).toFile());
        this.getArchiveBaseName().convention(new DefaultProvider<>(this.getProject()::getName));
        this.getArchiveVersion().convention(new DefaultProvider<>(() -> this.getProject().getVersion().toString()));
        this.getOutputs().upToDateWhen(t -> false);
        // We need dummy sources, or it will always skip with NO-SOURCE
        this.from(this.file);
    }
    
    @InputFile
    public RegularFile getFile() {
        return this.file.get();
    }

    public void setFile(RegularFile file) {
        this.file.set(file);
    }
    
    public void setFile(File file) {
        this.file.set(() -> file);
    }

    @Nonnull
    @Override
    protected CopyAction createCopyAction() {
        // Just do nothing
        return copy -> () -> true;
    }

    @TaskAction
    protected void copyFile(InputChanges inputs) throws IOException {
        Files.copy(this.getFile().getAsFile().toPath(), this.getArchiveFile().get().getAsFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
