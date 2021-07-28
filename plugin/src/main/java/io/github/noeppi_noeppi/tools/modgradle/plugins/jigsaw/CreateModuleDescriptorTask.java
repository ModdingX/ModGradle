package io.github.noeppi_noeppi.tools.modgradle.plugins.jigsaw;

import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateModuleDescriptorTask extends DefaultTask {

    private final Property<FileCollection> sources = this.getProject().getObjects().property(FileCollection.class);
    private final RegularFileProperty input = this.getProject().getObjects().fileProperty();
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();
    
    public CreateModuleDescriptorTask() {
        this.sources.convention(new DefaultProvider<>(() -> JavaEnv.getJavaSourceDirs(this.getProject())));
        this.input.convention(new DefaultProvider<RegularFile>(() -> {
            Set<File> dirs = JavaEnv.getJavaSources(this.getProject()).getJava().getSrcDirs();
            if (dirs.size() != 1)
                throw new IllegalStateException("Java SourceSet has multiple source directories. Please specify input file for " + this.getName() + " yourself.");
            File file = dirs.iterator().next().toPath().resolve("module").toFile();
            return () -> file;
        }));
        this.output.convention(new DefaultProvider<RegularFile>(() -> {
            Set<File> dirs = JavaEnv.getJavaSources(this.getProject()).getOutput().getGeneratedSourcesDirs().getFiles();
            if (dirs.size() != 1)
                throw new IllegalStateException("Java SourceSet has multiple generated output directories. Please specify output file for " + this.getName() + " yourself.");
            File file = dirs.iterator().next().toPath().resolve("module-info.java").toFile();
            return () -> file;
        }));
    }

    @InputFiles
    public FileCollection getSources() {
        return this.sources.get();
    }

    public void setSources(FileCollection sources) {
        this.sources.set(sources);
    }

    @InputFile
    public RegularFile getInput() {
        return this.input.get();
    }

    public void setInput(RegularFile input) {
        this.input.set(input);
    }

    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }
    
    @TaskAction
    protected void createModuleDescriptor(InputChanges inputs) throws IOException {
        Generator.generate(this.getInput().getAsFile().toPath(), this.getOutput().getAsFile().toPath(),
                this.getSources().getFiles().stream().map(File::toPath).collect(Collectors.toList()));
    }
}
