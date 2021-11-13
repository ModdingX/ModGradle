package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.*;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ExtractInheritanceTask extends JarExec {

    public ExtractInheritanceTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("inheritance", "--classes", "{classes}", "--classpath", "{classpath}", "--output", "{output}");
        this.getLibraryPath().convention(this.getProject().provider(() -> this.getProject().files()));
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
    }

    @InputDirectory
    public abstract DirectoryProperty getClasses();

    @InputFiles
    public abstract Property<FileCollection> getLibraryPath();

    @Input
    public abstract Property<Boolean> getGenerateLocals();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        List<String> built = new ArrayList<>(this.replaceArgs(args, Map.of(
                "{classes}", this.getClasses().getAsFile().get().toPath().toAbsolutePath().normalize().toString(),
                "{classpath}", this.getLibraryPath().get().getAsPath(),
                "{output}", this.getOutput().getAsFile().get().toPath().toAbsolutePath().normalize().toString()
        ), null));
        if (this.getGenerateLocals().get()) {
            built.add("--locals");
        }
        return built;
    }
}
