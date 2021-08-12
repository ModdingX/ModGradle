package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.*;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Map;

public abstract class ExtractLocalRenameMapTask extends JarExec {

    private final DirectoryProperty sources = this.getProject().getObjects().directoryProperty();
    private final RegularFileProperty inheritance = this.getProject().getObjects().fileProperty();
    private final RegularFileProperty transformer = this.getProject().getObjects().fileProperty();
    private final RegularFileProperty mappings = this.getProject().getObjects().fileProperty();
    private final Property<FileCollection> libraryPath = this.getProject().getObjects().property(FileCollection.class);
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();

    public ExtractLocalRenameMapTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("local", "--sources", "{sources}", "--inheritance", "{inheritance}", "--mappings", "{mappings}", "--transformer", "{transformer}", "--classpath", "{classpath}", "--remap", "--output", "{output}");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
        this.getOutputs().upToDateWhen(t -> false);
    }

    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        return this.replaceArgs(args, Map.of(
                "{sources}", this.getSources().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{inheritance}", this.getInheritance().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{mappings}", this.getMappings().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{transformer}", this.getTransformer().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{classpath}", this.getLibraryPath().getAsPath(),
                "{output}", this.getOutput().getAsFile().toPath().toAbsolutePath().normalize().toString()
        ), null);
    }

    @InputDirectory
    public Directory getSources() {
        return this.sources.get();
    }

    public void setSources(Directory classes) {
        this.sources.set(classes);
    }
    
    public void setSources(File classesDir) {
        this.sources.set(classesDir);
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
    
    @InputFile
    public RegularFile getTransformer() {
        return this.transformer.get();
    }

    public void setTransformer(RegularFile output) {
        this.transformer.set(output);
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
