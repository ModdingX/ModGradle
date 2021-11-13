package io.github.noeppi_noeppi.tools.modgradle.plugins.javadoc;

import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaHelper;
import io.github.noeppi_noeppi.tools.modgradle.util.PackageMatcher;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
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

public abstract class JavadocConfigureTask extends DefaultTask {

    public JavadocConfigureTask() {
        this.getSources().convention(JavaEnv.getJavaSourceDirs(this.getProject()));
        this.getExcludes().convention(this.getProject().provider(ArrayList::new));
        this.getIncludes().convention(this.getProject().provider(ArrayList::new));
        this.getOutputs().upToDateWhen(t -> false);
    }

    @InputFiles
    public abstract Property<FileCollection> getSources();

    @Input
    public abstract ListProperty<String> getExcludes();

    @Input
    public abstract ListProperty<String> getIncludes();

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
