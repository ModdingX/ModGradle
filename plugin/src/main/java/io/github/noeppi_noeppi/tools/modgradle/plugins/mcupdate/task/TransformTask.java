package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.api.task.ClasspathExec;
import io.github.noeppi_noeppi.tools.modgradle.util.ArgumentUtil;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class TransformTask extends ClasspathExec {

    public TransformTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("transform", "--inheritance", "{inheritance}", "--mappings", "{mappings}", "--transformer", "{transformer}", "--output", "{output}");
        this.getOutput().convention(this.getProject().provider(() -> () -> this.getProject().file("build").toPath().resolve(this.getName()).resolve("mappings.tsrg").toFile()));
    }
    
    @InputFile
    public abstract RegularFileProperty getInheritance();

    @Optional
    @InputFile
    public abstract RegularFileProperty getMappings();
    
    @Optional
    @InputFile
    public abstract RegularFileProperty getTransformer();
    
    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Override
    protected List<String> processArgs(List<String> args) {
        return ArgumentUtil.replaceArgs(args, Map.of(
                "inheritance", List.of(this.getInheritance()),
                "mappings", this.getMappings().isPresent() ? List.of(this.getMappings()) : List.of(),
                "transformer", this.getTransformer().isPresent() ? List.of(this.getTransformer()) : List.of(),
                "output", List.of(this.getOutput())
        ));
    }
}
