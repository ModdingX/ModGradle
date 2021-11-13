package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.mappings.MappingMerger;
import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class MergeMappingsTask extends DefaultTask {

    public MergeMappingsTask() {
        this.getMappings().convention(this.getProject().provider(() -> this.getProject().files()));
        this.getNoParam().convention(this.getProject().provider(() -> false));
    }

    @InputFile
    public abstract RegularFileProperty getPrimary();

    @InputFiles
    public abstract Property<FileCollection> getMappings();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Input
    public abstract Property<Boolean> getNoParam();

    @TaskAction
    protected void mergeMappings(InputChanges inputs) throws IOException {
        IMappingFile primary = IMappingFile.load(this.getPrimary().get().getAsFile());
        List<IMappingFile> mappings = new ArrayList<>();
        for (File file : this.getMappings().get()) {
            mappings.add(IMappingFile.load(file));
        }
        IMappingFile merged = MappingMerger.mergeMappings(primary, mappings, this.getNoParam().get());
        merged.write(this.getOutput().getAsFile().get().toPath(), IMappingFile.Format.TSRG2, false);
    }
}
