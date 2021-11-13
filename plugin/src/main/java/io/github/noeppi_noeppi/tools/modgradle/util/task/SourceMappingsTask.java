package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public abstract class SourceMappingsTask extends JarExec {
    
    public SourceMappingsTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("transform", "--inheritance", "{inheritance}", "--mappings", "{mappings}", "--output", "{output}");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
    }
    
    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        return this.replaceArgs(args, Map.of(
                "{inheritance}", this.getInheritance().getAsFile().get().toPath().toAbsolutePath().normalize().toString(),
                "{mappings}", this.getMappings().getAsFile().get().toPath().toAbsolutePath().normalize().toString(),
                "{output}", this.getOutput().getAsFile().get().toPath().toAbsolutePath().normalize().toString()
        ), null);
    }

    @InputFile
    public abstract RegularFileProperty getInheritance();

    @InputFile
    public abstract RegularFileProperty getMappings();

    @OutputFile
    public abstract RegularFileProperty getOutput();
}
