package org.moddingx.modgradle.util.task;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.api.task.ClasspathExec;
import org.moddingx.modgradle.util.ArgumentUtil;

import java.util.List;
import java.util.Map;

public abstract class SourceMappingsTask extends ClasspathExec {
    
    public SourceMappingsTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("transform", "--inheritance", "{inheritance}", "--mappings", "{mappings}", "--noparam", "--output", "{output}");
    }

    @InputFile
    public abstract RegularFileProperty getInheritance();

    @InputFile
    public abstract RegularFileProperty getMappings();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Override
    protected List<String> processArgs(List<String> args) {
        return ArgumentUtil.replaceArgs(args, Map.of(
                "inheritance", List.of(this.getInheritance()),
                "mappings", List.of(this.getMappings()),
                "output", List.of(this.getOutput())
        ));
    }
}
