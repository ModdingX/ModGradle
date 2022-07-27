package org.moddingx.modgradle.plugins.packdev.target;

import org.apache.commons.io.file.PathUtils;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.plugins.packdev.PackSettings;
import org.moddingx.modgradle.plugins.packdev.platform.ModFile;
import org.moddingx.modgradle.plugins.packdev.platform.ModdingPlatform;
import org.moddingx.modgradle.util.Side;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseTargetTask<T extends ModFile> extends AbstractArchiveTask {

    protected final ModdingPlatform<T> platform;
    protected final PackSettings settings;
    protected final List<T> files;

    private final Property<FileCollection> inputData = this.getProject().getObjects().property(FileCollection.class);

    @Inject
    public BaseTargetTask(ModdingPlatform<T> platform, PackSettings settings, List<T> files) {
        this.platform = platform;
        this.settings = settings;
        this.files = files;

        this.getArchiveExtension().convention(this.getProject().provider(() -> "zip"));

        this.inputData.convention(this.getProject().provider(() -> this.getProject().files(
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
        // Do nothing
        return copy -> () -> true;
    }

    @TaskAction
    public void generateOutput(InputChanges changes) throws IOException {
        Path target = this.getArchiveFile().get().getAsFile().toPath().toAbsolutePath().normalize();
        if (!Files.exists(target.getParent())) Files.createDirectories(target.getParent());
        if (Files.exists(target)) Files.delete(target);
        this.generate(target);
    }

    protected abstract void generate(Path target) throws IOException;

    // Elements later in the list should overwrite
    // Null means everything
    protected final List<Path> getOverridePaths(@Nullable Side side) {
        List<Path> list = new ArrayList<>();
        if (side != null) {
            list.add(this.getProject().file("data/" + Side.COMMON.id).toPath());
            if (side != Side.COMMON) list.add(this.getProject().file("data/" + side.id).toPath());
        } else {
            list.add(this.getProject().file("data/" + Side.COMMON.id).toPath());
            list.add(this.getProject().file("data/" + Side.SERVER.id).toPath());
            list.add(this.getProject().file("data/" + Side.CLIENT.id).toPath());
        }

        return list.stream().filter(Files::isDirectory).toList();
    }
    
    protected final void copyAllDataTo(Path target, @Nullable Side side) throws IOException {
        if (!Files.isDirectory(target)) {
            Files.createDirectories(target);
        }
        for (Path src : this.getOverridePaths(side)) {
            PathUtils.copyDirectory(src, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    protected final void copyOverrideDataTo(Path target, Side side) throws IOException {
        if (!Files.isDirectory(target)) {
            Files.createDirectories(target);
        }
        PathUtils.copyDirectory(this.getProject().file("data/" + side.id).toPath(), target, StandardCopyOption.REPLACE_EXISTING);
    }
}
