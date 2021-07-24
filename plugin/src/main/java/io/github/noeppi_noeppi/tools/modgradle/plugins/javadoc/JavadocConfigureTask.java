package io.github.noeppi_noeppi.tools.modgradle.plugins.javadoc;

import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaHelper;
import io.github.noeppi_noeppi.tools.modgradle.util.PackageMatcher;
import io.github.noeppi_noeppi.tools.modgradle.util.StringUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JavadocConfigureTask extends DefaultTask {
    
    private final Property<FileCollection> sources = this.getProject().getObjects().property(FileCollection.class);
    private final ListProperty<String> excludes = this.getProject().getObjects().listProperty(String.class);
    private final ListProperty<String> includes = this.getProject().getObjects().listProperty(String.class);
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();
    
    public JavadocConfigureTask() {
        this.sources.convention(new DefaultProvider<>(() -> JavaEnv.getJavaSourceDirs(this.getProject())));
        this.excludes.convention(new DefaultProvider<>(ArrayList::new));
        this.includes.convention(new DefaultProvider<>(ArrayList::new));
        this.output.convention(new DefaultProvider<>(() -> (RegularFile) () -> this.getProject().file("build").toPath().resolve(this.getName()).resolve("options.txt").toFile()));
    }

    @InputFiles
    public FileCollection getSources() {
        return this.sources.get();
    }

    public void setSources(FileCollection sources) {
        this.sources.set(sources);
    }

    @Input
    public List<String> getExcludes() {
        return this.excludes.get();
    }

    public void setExcludes(List<String> excludes) {
        this.excludes.set(excludes);
    }
    
    public void exclude(String exclude) {
        this.includes.get().add(exclude);
    }

    @Input
    public List<String> getIncludes() {
        return this.includes.get();
    }

    public void setIncludes(List<String> includes) {
        this.includes.set(includes);
    }
    
    public void include(String include) {
        this.includes.get().add(include);
    }
    
    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }
    
    @TaskAction
    protected void configureJavadoc(InputChanges inputs) throws IOException {
        
        Set<String> packages = JavaHelper.findPackages(this.getSources().getFiles().stream().map(File::toPath).collect(Collectors.toList()));
        PackageMatcher matcher = new PackageMatcher(this.getExcludes(), this.getIncludes());
        Set<String> excluded = packages.stream().filter(matcher.getMatcher()).collect(Collectors.toSet());

        Path output = this.getOutput().getAsFile().toPath();
        if (!Files.isDirectory(output.getParent())) {
            Files.createDirectories(output.getParent());
        }
        Writer writer = Files.newBufferedWriter(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        for (String pkg : excluded) {
            writer.write(" -exclude " + StringUtil.quote(pkg));
        }
        writer.write("\n");
        writer.close();
    }
}
