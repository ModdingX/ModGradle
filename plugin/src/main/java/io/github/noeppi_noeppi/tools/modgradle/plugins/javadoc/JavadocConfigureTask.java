package io.github.noeppi_noeppi.tools.modgradle.plugins.javadoc;

import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaHelper;
import io.github.noeppi_noeppi.tools.modgradle.util.PackageMatcher;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class JavadocConfigureTask extends DefaultTask {
    
    private final Property<FileCollection> sources = this.getProject().getObjects().property(FileCollection.class);
    private final ListProperty<String> excludes = this.getProject().getObjects().listProperty(String.class);
    private final ListProperty<String> includes = this.getProject().getObjects().listProperty(String.class);

    public JavadocConfigureTask() {
        this.sources.convention(new DefaultProvider<>(() -> JavaEnv.getJavaSourceDirs(this.getProject())));
        this.excludes.convention(new DefaultProvider<>(ArrayList::new));
        this.includes.convention(new DefaultProvider<>(ArrayList::new));
        this.getOutputs().upToDateWhen(t -> false);
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
        try {
            this.excludes.add(exclude);
        } catch (UnsupportedOperationException e) {
            // add seems to randomly fail sometimes
            ArrayList<String> list = new ArrayList<>(this.excludes.get());
            list.add(exclude);
            this.excludes.set(list);
        }
    }

    @Input
    public List<String> getIncludes() {
        return this.includes.get();
    }

    public void setIncludes(List<String> includes) {
        this.includes.set(includes);
    }

    public void include(String include) {
        try {
            this.includes.add(include);
        } catch (UnsupportedOperationException e) {
            // add seems to randomly fail sometimes
            ArrayList<String> list = new ArrayList<>(this.excludes.get());
            list.add(include);
            this.includes.set(list);
        }
    }

    public FileCollection getDirs(Path base) {
        try {
            Set<String> packages = JavaHelper.findPackages(List.of(base));
            PackageMatcher matcher = new PackageMatcher(this.getExcludes(), this.getIncludes());
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

    private static Stream<Path> listDir(Path path) {
        try {
            return Files.list(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @TaskAction
    protected void configureJavadoc(InputChanges inputs) throws IOException {
        //
    }
}
