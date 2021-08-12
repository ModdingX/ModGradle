package io.github.noeppi_noeppi.tools.modgradle.api;

import io.github.noeppi_noeppi.tools.modgradle.util.JavaHelper;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaCompiler;

import java.nio.file.Paths;

/**
 * Utilities for java source.
 */
public class JavaEnvironment {

    /**
     * Gets the library path (classpath + system libraries) for a java compile task.
     */
    public static FileCollection getLibraryPath(Project project, JavaCompile task) {
        ConfigurableFileCollection files = project.files();
        files.from(task.getClasspath());
        if (task.getJavaCompiler().getOrNull() != null) {
            JavaHelper.addBuiltinLibraries(project, task.getJavaCompiler().get().getMetadata().getInstallationPath().getAsFile().toPath(), files);
        } else {
            JavaHelper.addBuiltinLibraries(project, Paths.get(System.getProperty("java.home")), files);
        }
        return files;
    }
    
    /**
     * Gets the system libraries for a java compiler.
     */
    public static FileCollection getSystemLibraries(Project project, JavaCompiler compiler) {
        ConfigurableFileCollection files = project.files();
        JavaHelper.addBuiltinLibraries(project, compiler.getMetadata().getInstallationPath().getAsFile().toPath(), files);
        return files;
    }
}
