package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.*;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SanitizeParchmentTask extends JarExec {

    private final RegularFileProperty inheritance = this.getProject().getObjects().fileProperty();
    private final RegularFileProperty input = this.getProject().getObjects().fileProperty();
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();
    private final Property<FileCollection> libraryPath = this.getProject().getObjects().property(FileCollection.class);
    private final DirectoryProperty sources = this.getProject().getObjects().directoryProperty();
    private final SetProperty<String> ignored = this.getProject().getObjects().setProperty(String.class);

    public SanitizeParchmentTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("sanitize", "--inheritance", "{inheritance}", "--input", "{input}", "--output", "{output}", "--classpath", "{classpath}", "--sources", "{sources}", "--quiet", "--ignore", "{ignored}");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
    }
    
    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        return this.replaceArgs(args, Map.of(
                "{inheritance}", this.getInheritance().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{input}", this.getInput().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{output}", this.getOutput().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{sources}", this.getSources().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{classpath}", this.getLibraryPath().getAsPath()
        ), Map.of(
                "{ignored}", this.getIgnoredPackages()
        ));
    }
    
    @InputFile
    public RegularFile getInheritance() {
        return this.inheritance.get();
    }
    
    public void setInheritance(RegularFile output) {
        this.inheritance.set(output);
    }
    
    public void setInheritance(File output) {
        this.inheritance.set(output);
    }
    
    @InputFile
    public RegularFile getInput() {
        return this.input.get();
    }

    public void setInput(RegularFile input) {
        this.input.set(input);
    }
    
    public void setInput(File input) {
        this.input.set(input);
    }

    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }
    
    public void setOutput(File output) {
        this.output.set(output);
    }
    
    @InputFiles
    public FileCollection getLibraryPath() {
        return this.libraryPath.get();
    }

    public void setLibraryPath(FileCollection libraryPath) {
        this.libraryPath.set(libraryPath);
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
    
    @Input
    public Set<String> getIgnoredPackages() {
        return this.ignored.get();
    }
    
    public void setIgnoredPackages(Set<String> ignored) {
        this.ignored.set(ignored);
    }
    
    public void ignore(String pkg) {
        this.ignored.add(pkg);
    }
}
