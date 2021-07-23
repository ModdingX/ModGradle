package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.*;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public abstract class ExtractInheritanceTask extends JarExec {

    private final DirectoryProperty classes = this.getProject().getObjects().directoryProperty();
    private final Property<FileCollection> libraryPath = this.getProject().getObjects().property(FileCollection.class);
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();
    
    public ExtractInheritanceTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("inheritance", "--classes", "{classes}", "--classpath", "{classpath}", "--locals", "--output", "{output}");
        this.libraryPath.convention(new DefaultProvider<>(() -> this.getProject().files()));
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
        this.getOutputs().upToDateWhen(t -> false);
    }
    
    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        return this.replaceArgs(args, Map.of(
                "{classes}", this.getClasses().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{classpath}", this.getLibraryPath().getAsPath(),
                "{output}", this.getOutput().getAsFile().toPath().toAbsolutePath().normalize().toString()
        ), null);
    }

    @InputDirectory
    public Directory getClasses() {
        return this.classes.get();
    }

    public void setClasses(Directory classes) {
        this.classes.set(classes);
    }

    @InputFiles
    public FileCollection getLibraryPath() {
        return this.libraryPath.get();
    }

    public void setLibraryPath(FileCollection libraryPath) {
        this.libraryPath.set(libraryPath);
    }

    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }
}
