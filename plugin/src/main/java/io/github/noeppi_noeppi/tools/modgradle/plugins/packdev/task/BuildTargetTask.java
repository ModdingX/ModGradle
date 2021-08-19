package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task;

import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackSettings;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.work.InputChanges;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class BuildTargetTask extends AbstractArchiveTask {

    protected final PackSettings settings;
    protected final List<CurseFile> files;

    private final Property<FileCollection> inputData = this.getProject().getObjects().property(FileCollection.class);

    @Inject
    public BuildTargetTask(PackSettings settings, List<CurseFile> files) {
        this.settings = settings;
        this.files = files;
        
        this.getArchiveBaseName().convention(new DefaultProvider<>(this.getProject()::getName));
        this.getArchiveVersion().convention(new DefaultProvider<>(() -> this.getProject().getVersion().toString()));
        this.getArchiveClassifier().convention(new DefaultProvider<>(() -> ""));
        this.getArchiveExtension().convention(new DefaultProvider<>(() -> "zip"));
        
        this.inputData.convention(new DefaultProvider<>(() -> this.getProject().files(
                this.getProject().file("modlist.json"),
                this.getProject().file("data/" + Side.COMMON.id),
                this.getProject().file("data/" + Side.CLIENT.id),
                this.getProject().file("data/" + Side.SERVER.id)
        )));
        // We need dummy sources, or it will always skip with NO-SOURCE
        this.from(this.inputData);
    }

    @InputFiles
    public FileCollection getInputData() {
        return this.inputData.get();
    }

    public void setInputData(FileCollection inputMods) {
        this.inputData.set(inputMods);
    }

    @Nonnull
    @Override
    protected CopyAction createCopyAction() {
        // Just do nothing
        return copy -> () -> true;
    }

    @TaskAction
    public void generateOutput(InputChanges changes) throws IOException {
        Path target = this.getArchiveFile().get().getAsFile().toPath();
        if (!Files.exists(target.getParent())) Files.createDirectories(target.getParent());
        if (Files.exists(target)) Files.delete(target);
        this.generate(target);
    }
    
    protected abstract void generate(Path target) throws IOException;
}
