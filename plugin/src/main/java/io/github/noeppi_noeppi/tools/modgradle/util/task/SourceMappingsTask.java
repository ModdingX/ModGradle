package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public abstract class SourceMappingsTask extends JarExec {

    private final RegularFileProperty inheritance = this.getProject().getObjects().fileProperty();
    private final RegularFileProperty mappings = this.getProject().getObjects().fileProperty();
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();
    
    public SourceMappingsTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("transform", "--inheritance", "{inheritance}", "--mappings", "{mappings}", "--output", "{output}");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
        this.getOutputs().upToDateWhen(t -> false);
    }
    
    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        return this.replaceArgs(args, Map.of(
                "{inheritance}", this.getInheritance().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{mappings}", this.getMappings().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{output}", this.getOutput().getAsFile().toPath().toAbsolutePath().normalize().toString()
        ), null);
    }

    @InputFile
    public RegularFile getInheritance() {
        return this.inheritance.get();
    }

    public void setInheritance(RegularFile output) {
        this.inheritance.set(output);
    }

    @InputFile
    public RegularFile getMappings() {
        return this.mappings.get();
    }

    public void setMappings(RegularFile output) {
        this.mappings.set(output);
    }

    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }
}
