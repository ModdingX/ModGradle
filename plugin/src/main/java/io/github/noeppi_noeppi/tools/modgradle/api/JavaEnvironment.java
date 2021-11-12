package io.github.noeppi_noeppi.tools.modgradle.api;

import io.github.noeppi_noeppi.tools.modgradle.util.JavaHelper;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
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
    public static Provider<FileCollection> getLibraryPath(Project project, JavaCompile task) {
        return project.provider(() -> {
            ConfigurableFileCollection fc = project.files();
            fc.from(task.getClasspath());
            if (task.getJavaCompiler().getOrNull() != null) {
                JavaHelper.addBuiltinLibraries(project, task.getJavaCompiler().get().getMetadata().getInstallationPath().getAsFile().toPath(), fc);
            } else {
                JavaHelper.addBuiltinLibraries(project, Paths.get(System.getProperty("java.home")), fc);
            }
            return fc;
        });
    }
    
    /**
     * Gets the system libraries for a java compiler.
     */
    public static Provider<FileCollection> getSystemLibraries(Project project, JavaCompiler compiler) {
        return project.provider(() -> {
            ConfigurableFileCollection fc = project.files();
            JavaHelper.addBuiltinLibraries(project, compiler.getMetadata().getInstallationPath().getAsFile().toPath(), fc);
            return fc;
        });
    }
}
