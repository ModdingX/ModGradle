package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ExtractLocalTask extends JarExec {
    
    public ExtractLocalTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("local", "--inheritance", "{inheritance}", "--sources", "{sources}", "--classpath", "{classpath}", "--transformer", "{transformer}", "--output", "{output}");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
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

    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        List<String> built = new ArrayList<>(this.replaceArgs(args, Map.of(
                "{inheritance}", this.getInheritance().getAsFile().get().toPath().toAbsolutePath().normalize().toString(),
                "{sources}", this.getSources().get().getAsPath(),
                "{classpath}", this.getLibraryPath().get().getAsPath(),
                "{transformer}", this.getTransformer().get().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{output}", this.getOutput().getAsFile().get().toPath().toAbsolutePath().normalize().toString()
        ), null));
        if (this.getMappings().isPresent()) {
            built.addAll(List.of("--mappings", this.getMappings().get().getAsFile().toPath().toAbsolutePath().normalize().toString()));
        }
        return built;
    }
}
