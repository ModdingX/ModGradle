package org.moddingx.modgradle.plugins.javadoc;

import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.util.JavaGradlePluginUtils;
import org.moddingx.modgradle.util.JavaSourceTreeUtils;
import org.moddingx.modgradle.util.PackageMatcher;
import org.moddingx.modgradle.util.TaskUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public abstract class JavadocConfigureTask extends DefaultTask {
    
    public JavadocConfigureTask() {
        this.getSources().convention(JavaGradlePluginUtils.getJavaSourceDirs(this.getProject()));
        this.getExcludes().convention(this.getProject().provider(ArrayList::new));
        this.getIncludes().convention(this.getProject().provider(ArrayList::new));
        this.getDocletMetaOptionsFile().convention(TaskUtils.defaultOutputFile(this, "metaOptions.txt"));
        this.getOutputs().upToDateWhen(t -> false);
    }
    
    @InputFiles
    public abstract Property<FileCollection> getSources();

    @Input
    public abstract ListProperty<String> getExcludes();

    @Input
    public abstract ListProperty<String> getIncludes();
    
    @OutputFile
    public abstract RegularFileProperty getDocletMetaOptionsFile();
    
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

    @Internal
    public FileCollection getJavadocTaskInputs() throws IOException {
        try {
            return this.getProject().files(this.getJavaSourceDirectories().stream()
                    .map(this::getJavadocTaskInputs)
                    .toArray());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
    
    private List<Path> getJavaSourceDirectories() {
        return this.getSources().get().getFiles().stream()
                .map(File::toPath)
                .map(path -> path.toAbsolutePath().normalize().toAbsolutePath())
                .toList();
    }
    
    private FileCollection getJavadocTaskInputs(Path base) throws UncheckedIOException {
        try {
            Set<String> packages = JavaSourceTreeUtils.findPackages(List.of(base));
            return this.getProject().files(packages.stream()
                    .filter(this.getExcludedPackageMatcher().negate())
                    .map(pkg -> pkg.replace(".", base.getFileSystem().getSeparator()))
                    .map(base::resolve)
                    .map(path -> path.toAbsolutePath().normalize().toAbsolutePath())
                    .filter(Files::isDirectory)
                    .flatMap(JavadocConfigureTask::listDir)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile).toArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private PackageMatcher getExcludedPackageMatcher() {
        return new PackageMatcher(this.getExcludes().get(), this.getIncludes().get());
    }

    private static Stream<Path> listDir(Path path) {
        try (Stream<Path> dirStream = Files.list(path)) {
            return dirStream.toList().stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @TaskAction
    protected void configureJavadoc(InputChanges inputs) throws IOException {
        // JavaDocletMeta gets the full sources as they are needed to correctly generate the metadata.
        // Exclude logic is done through arguments.
        Set<String> allPackages = JavaSourceTreeUtils.findPackages(this.getJavaSourceDirectories());
        List<String> excludedPackages = allPackages.stream().filter(this.getExcludedPackageMatcher()).sorted().toList();
        
        Path metaOptions = this.getDocletMetaOptionsFile().get().getAsFile().toPath();
        PathUtils.createParentDirectories(metaOptions);
        try (BufferedWriter writer = Files.newBufferedWriter(metaOptions, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String excludedPackage : excludedPackages) {
                writer.write(" --exclude-package " + excludedPackage);
            }
            writer.write("\n");
        }
    }
}
