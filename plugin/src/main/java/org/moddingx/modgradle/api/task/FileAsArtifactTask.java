package org.moddingx.modgradle.api.task;

import org.apache.commons.io.file.PathUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.work.InputChanges;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Wrap a file into an archive task, so it can be used by maven-publish.
 */
public abstract class FileAsArtifactTask extends AbstractArchiveTask {
    
    public FileAsArtifactTask() {
        this.getDestinationDirectory().set(this.getProject().file("build").toPath().resolve(this.getName()).toFile());
        this.getArchiveBaseName().convention(this.getProject().provider(this.getProject()::getName));
        this.getArchiveVersion().convention(this.getProject().provider(() -> this.getProject().getVersion().toString()));
        this.getOutputs().upToDateWhen(t -> false);
        this.from(this.getFile());
    }

    /**
     * Gets the path of the file to wrap.
     */
    @InputFile
    public abstract RegularFileProperty getFile();
    
    @Nonnull
    @Override
    protected CopyAction createCopyAction() {
        // Just do nothing
        return copy -> () -> true;
    }

    @TaskAction
    protected void copyFile(InputChanges inputs) throws IOException {
        Path target = this.getArchiveFile().get().getAsFile().toPath();
        PathUtils.createParentDirectories(target);
        Files.copy(this.getFile().getAsFile().get().toPath(), target, StandardCopyOption.REPLACE_EXISTING);
    }
}
