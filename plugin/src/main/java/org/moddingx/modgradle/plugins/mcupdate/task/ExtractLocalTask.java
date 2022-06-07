package org.moddingx.modgradle.plugins.mcupdate.task;

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.api.task.ClasspathExec;
import org.moddingx.modgradle.util.ArgumentUtil;

import java.util.List;
import java.util.Map;

public abstract class ExtractLocalTask extends ClasspathExec {
    
    public ExtractLocalTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("local", "--inheritance", "{inheritance}", "--sources", "{sources}", "--classpath", "{classpath}", "--transformer", "{transformer}", "--mappings", "{mappings}", "--output", "{output}");
        this.getOutput().convention(this.getProject().provider(() -> () -> this.getProject().file("build").toPath().resolve(this.getName()).resolve("local.txt").toFile()));
    }
    
    @InputFile
    public abstract RegularFileProperty getInheritance();

    @InputFiles
    public abstract Property<FileCollection> getSources();
    
    @InputFiles
    public abstract Property<FileCollection> getLibraryPath();

    @Optional
    @InputFile
    public abstract RegularFileProperty getMappings();
    
    @InputFile
    public abstract RegularFileProperty getTransformer();
    
    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Override
    protected List<String> processArgs(List<String> args) {
        return ArgumentUtil.replaceArgs(args, Map.of(
                "inheritance", List.of(this.getInheritance()),
                "sources", List.of(this.getSources()),
                "classpath", List.of(this.getLibraryPath()),
                "transformer", List.of(this.getTransformer()),
                "mappings", this.getMappings().isPresent() ? List.of(this.getMappings()) : List.of(),
                "output", List.of(this.getOutput())
        ));
    }
}
