package io.github.noeppi_noeppi.tools.modgradle.plugins.javadoc;

import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaHelper;
import io.github.noeppi_noeppi.tools.modgradle.util.PackageMatcher;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public abstract class JavadocConfigureTask extends DefaultTask {

    public JavadocConfigureTask() {
        this.getSources().convention(JavaEnv.getJavaSourceDirs(this.getProject()));
        this.getExcludes().convention(this.getProject().provider(ArrayList::new));
        this.getIncludes().convention(this.getProject().provider(ArrayList::new));
        this.getDocletMetaOptions().convention(this.getProject().provider(() -> () -> this.getProject().file("build").toPath().resolve(this.getName()).resolve("metaOptions.txt").toFile()));
        this.getOutputs().upToDateWhen(t -> false);
    }

    @InputFiles
    public abstract Property<FileCollection> getSources();

    @Input
    public abstract ListProperty<String> getExcludes();

    @Input
    public abstract ListProperty<String> getIncludes();
    
    @OutputFile
    public abstract RegularFileProperty getDocletMetaOptions();
    
    public void from(Object from) {
        FileCollection old = this.getSources().get();
        this.getSources().set(this.getProject().files(from, old));
    }
    
    public void exclude(String pattern) {
        this.getExcludes().add(pattern);
    }
    
    public void include(String pattern) {
        this.getIncludes().add(pattern);
    }

    public FileCollection getDirs(Path base) {
        try {
            Set<String> packages = JavaHelper.findPackages(List.of(base));
            PackageMatcher matcher = new PackageMatcher(this.getExcludes().get(), this.getIncludes().get());
            return this.getProject().files(packages.stream()
                    .filter(matcher.getMatcher().negate())
                    .map(pkg -> pkg.replace('.', '/'))
                    .map(base::resolve)
                    .map(path -> path.toAbsolutePath().normalize())
                    .filter(Files::isDirectory)
                    .flatMap(JavadocConfigureTask::listDir)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile).toArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public List<String> getExcludedPackages(Path base) {
        try {
            Set<String> packages = JavaHelper.findPackages(List.of(base));
            PackageMatcher matcher = new PackageMatcher(this.getExcludes().get(), this.getIncludes().get());
            return packages.stream().filter(matcher.getMatcher()).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Path> listDir(Path path) {
        try {
            return Files.list(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @TaskAction
    protected void configureJavadoc(InputChanges inputs) throws IOException {
        List<String> excluded = this.getSources().get().getFiles().stream()
                .map(File::toPath)
                .map(path -> path.toAbsolutePath().normalize())
                .flatMap(p -> this.getExcludedPackages(p).stream())
                .distinct().toList();
        
        Path metaOptions = this.getDocletMetaOptions().get().getAsFile().toPath();
        PathUtils.createParentDirectories(metaOptions);
        try (BufferedWriter writer = Files.newBufferedWriter(metaOptions, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String ex : excluded) {
                writer.write(" --exclude-package " + ex);
            }
            writer.write("\n");
        }
    }
}
