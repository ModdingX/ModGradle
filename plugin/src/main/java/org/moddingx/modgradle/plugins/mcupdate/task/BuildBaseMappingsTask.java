package org.moddingx.modgradle.plugins.mcupdate.task;

import com.google.common.collect.Streams;
import net.minecraftforge.srgutils.IMappingFile;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.mappings.MappingMerger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

public abstract class BuildBaseMappingsTask extends DefaultTask {

    public BuildBaseMappingsTask() {
        this.getAdditionalMappings().convention(this.getProject().provider(ArrayList::new));
        this.getMappingOutput().convention(this.getProject().provider(() -> () -> this.getProject().file("build").toPath().resolve(this.getName()).resolve("mappings.tsrg").toFile()));
    }

    @Input
    @Optional
    public abstract Property<URL> getMainMappings();

    @Input
    @Optional
    public abstract ListProperty<URL> getAdditionalMappings();

    @OutputFile
    public abstract RegularFileProperty getMappingOutput();

    @TaskAction
    public void buildMappings(InputChanges changes) throws IOException {
        Path path = this.getMappingOutput().get().getAsFile().toPath();
        PathUtils.createParentDirectories(path);

        IMappingFile merged = MappingMerger.mergeMappings(Streams.concat(
                Stream.ofNullable(this.getMainMappings().getOrNull()).map(this::loadMappings),
                this.getAdditionalMappings().get().stream().map(this::loadMappings)
        ).toList(), false);
        merged.write(path, IMappingFile.Format.TSRG2, false);
    }

    private IMappingFile loadMappings(URL url) {
        try (InputStream in = url.openStream()) {
            return IMappingFile.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mappings from " + url, e);
        }
    }
}
