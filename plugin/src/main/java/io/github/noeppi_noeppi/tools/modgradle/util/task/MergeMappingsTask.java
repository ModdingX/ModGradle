package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.mappings.MappingMerger;
import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MergeMappingsTask extends DefaultTask {

    private final RegularFileProperty primary = this.getProject().getObjects().fileProperty();
    private final Property<FileCollection> mappings = this.getProject().getObjects().property(FileCollection.class);
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();

    public MergeMappingsTask() {
        this.mappings.convention(new DefaultProvider<>(() -> this.getProject().files()));
    }

    @InputFile
    public RegularFile getPrimary() {
        return this.primary.get();
    }

    public void setPrimary(RegularFile output) {
        this.primary.set(output);
    }
    
    @InputFiles
    public FileCollection getMappings() {
        return this.mappings.get();
    }

    public void setMappings(FileCollection libraryPath) {
        this.mappings.set(libraryPath);
    }

    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }
    
    @TaskAction
    protected void mergeMappings(InputChanges inputs) throws IOException {
        IMappingFile primary = IMappingFile.load(this.getPrimary().getAsFile());
        List<IMappingFile> mappings = new ArrayList<>();
        for (File file : this.getMappings()) {
            mappings.add(IMappingFile.load(file));
        }
        IMappingFile merged = MappingMerger.mergeMappings(primary, mappings);
        merged.write(this.getOutput().getAsFile().toPath(), IMappingFile.Format.TSRG2, false);
    }
}
