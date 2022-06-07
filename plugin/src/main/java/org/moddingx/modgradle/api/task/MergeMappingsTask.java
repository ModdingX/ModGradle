package org.moddingx.modgradle.api.task;

import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.mappings.MappingMerger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A task to merge multiple mapping files together. It merges some primary mappings together to some additional
 * mappings. Elements from the additional mappings will replace elements from the primary mappings. Elements from
 * mapping files specified later in the list of additional mappings will also replace elements from mappings
 * specified earlier.
 */
public abstract class MergeMappingsTask extends DefaultTask {

    public MergeMappingsTask() {
        this.getAdditional().convention(this.getProject().provider(() -> this.getProject().files()));
        this.getNoParam().convention(this.getProject().provider(() -> false));
        this.getFormat().convention("tsrg2");
        this.getOutput().convention(this.getProject().provider(() -> () -> this.getProject().file("build").toPath().resolve(this.getName()).resolve("mappings.tsrg").toFile()));
    }

    /**
     * The primary mappings to merge. This is optional an can be left out.
     * If it is left out and no additional mappings are specified, an empty mapping file is produced.
     */
    @InputFile
    @Optional
    public abstract RegularFileProperty getPrimary();

    /**
     * The additional mappings to merge with the primary mappings.
     */
    @InputFiles
    public abstract Property<FileCollection> getAdditional();

    /**
     * The output format. Default is tsrg2.
     * Supported values are: srg, xsrg, csrg, tsrg, tsrg2, pg (ProGuard), tiny1, tiny (for tiny 2)
     */
    @Input
    public abstract Property<String> getFormat();

    /**
     * The output file for the merged mappings
     */
    @OutputFile
    public abstract RegularFileProperty getOutput();

    /**
     * Whether params should be left out of the merged mappings. Default is {@code false}.
     */
    @Input
    public abstract Property<Boolean> getNoParam();

    @TaskAction
    protected void mergeMappings(InputChanges inputs) throws IOException {
        IMappingFile.Format format = IMappingFile.Format.get(this.getFormat().get());
        if (format == null) {
            throw new IOException("Unknown mapping format: " + this.getFormat().get());
        }
        List<IMappingFile> mappings = new ArrayList<>();
        RegularFile primary = this.getPrimary().getOrNull();
        if (primary != null) {
            mappings.add(IMappingFile.load(primary.getAsFile()));
        }
        for (File file : this.getAdditional().get()) {
            mappings.add(IMappingFile.load(file));
        }
        IMappingFile merged = MappingMerger.mergeMappings(mappings, this.getNoParam().get());
        merged.write(this.getOutput().getAsFile().get().toPath(), IMappingFile.Format.TSRG2, false);
    }
}
