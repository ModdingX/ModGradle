package org.moddingx.modgradle.plugins.mcupdate.task;

import net.minecraftforge.srgutils.IMappingBuilder;
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
import org.moddingx.launcherlib.mappings.MappingHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;

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
    public void buildMappings(InputChanges inputs) throws IOException {
        Path path = this.getMappingOutput().get().getAsFile().toPath();
        PathUtils.createParentDirectories(path);

        IMappingFile main = this.getMainMappings().map(this::loadMappings).getOrNull();
        if (main == null) main = IMappingBuilder.create("from", "to").build().getMap("from", "to");

        IMappingFile builtMappings = main;
        if (!this.getAdditionalMappings().get().isEmpty()) {
            IMappingFile mergedAdditional = MappingHelper.merge(this.getAdditionalMappings().get().stream().map(this::loadMappings).toList());
            
            // Do combination of chain and merge
            IMappingFile chainedMain = main.chain(mergedAdditional);
            builtMappings = MappingHelper.merge(mergedAdditional, chainedMain);
        }
        
        builtMappings.write(path, IMappingFile.Format.TSRG2, false);
    }

    private IMappingFile loadMappings(URL url) {
        try (InputStream in = url.openStream()) {
            return IMappingFile.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mappings from " + url, e);
        }
    }
}
