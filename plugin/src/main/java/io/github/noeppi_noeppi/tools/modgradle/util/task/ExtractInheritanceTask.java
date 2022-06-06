package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.api.task.ClasspathExec;
import io.github.noeppi_noeppi.tools.modgradle.util.ArgumentUtil;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;

import java.util.List;
import java.util.Map;

public abstract class ExtractInheritanceTask extends ClasspathExec {

    public ExtractInheritanceTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("inheritance", "--classes", "{classes}", "--classpath", "{classpath}", "--output", "{output}");
        this.getLibraryPath().convention(this.getProject().provider(() -> this.getProject().files()));
        this.getOutput().convention(this.getProject().provider(() -> () -> this.getProject().file("build").toPath().resolve(this.getName()).resolve("inheritance.txt").toFile()));
    }

    @InputDirectory
    public abstract DirectoryProperty getClasses();

    @InputFiles
    public abstract Property<FileCollection> getLibraryPath();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Override
    protected List<String> processArgs(List<String> args) {
        return ArgumentUtil.replaceArgs(args, Map.of(
                "classes", List.of(this.getClasses()),
                "classpath", List.of(this.getLibraryPath()),
                "output", List.of(this.getOutput())
        ));
    }
}
