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

public abstract class RemapInheritanceTask extends JarExec {

    private final RegularFileProperty input = this.getProject().getObjects().fileProperty();
    private final RegularFileProperty mappings = this.getProject().getObjects().fileProperty();
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();
    
    public RemapInheritanceTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("remap", "--input", "{inheritance}", "--mappings", "{mappings}", "--output", "{output}");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
        this.getOutputs().upToDateWhen(t -> false);
    }
    
    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        return this.replaceArgs(args, Map.of(
                "{inheritance}", this.getInput().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{mappings}", this.getMappings().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{output}", this.getOutput().getAsFile().toPath().toAbsolutePath().normalize().toString()
        ), null);
    }

    @InputFile
    public RegularFile getInput() {
        return this.input.get();
    }

    public void setInput(RegularFile input) {
        this.input.set(input);
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
