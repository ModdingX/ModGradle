package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class TransformTask extends JarExec {

    public TransformTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("transform", "--inheritance", "{inheritance}", "--output", "{output}");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
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

    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        List<String> built = new ArrayList<>(this.replaceArgs(args, Map.of(
                "{inheritance}", this.getInheritance().getAsFile().get().toPath().toAbsolutePath().normalize().toString(),
                "{output}", this.getOutput().getAsFile().get().toPath().toAbsolutePath().normalize().toString()
        ), null));
        if (this.getMappings().isPresent()) {
            built.addAll(List.of("--mappings", this.getMappings().get().getAsFile().toPath().toAbsolutePath().normalize().toString()));
        }
        if (this.getTransformer().isPresent()) {
            built.addAll(List.of("--transformer", this.getTransformer().get().getAsFile().toPath().toAbsolutePath().normalize().toString()));
        }
        return built;
    }
}
