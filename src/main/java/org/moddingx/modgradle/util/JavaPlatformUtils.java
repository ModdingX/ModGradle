package org.moddingx.modgradle.util;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.jvm.Jvm;
import org.gradle.jvm.toolchain.JavaLauncher;

import java.nio.file.Files;
import java.nio.file.Path;

public class JavaPlatformUtils {
    
    public static Path findJavaHome(JavaCompile compileTask) {
        if (compileTask.getJavaCompiler().getOrNull() != null) {
            return compileTask.getJavaCompiler().get().getMetadata().getInstallationPath().getAsFile().toPath();
        } else {
            return getSystemJavaHome();
        }
    }
    
    public static Path findJavaHome(Provider<JavaLauncher> launcher) {
        Path javaHome = launcher.get().getMetadata().getInstallationPath().getAsFile().toPath();
        if (Files.isDirectory(javaHome)) {
            return javaHome;
        } else {
            return getSystemJavaHome();
        }
    }
    
    private static Path getSystemJavaHome() {
        Path javaHome = Path.of(System.getProperty("java.home"));
        if (Files.isDirectory(javaHome)) return javaHome;
        return Jvm.current().getJavaHome().toPath();
    }
    
    public static FileCollection systemLibraryPath(Project project, Path javaHome) {
        ConfigurableFileCollection fc = project.files();
        addSystemLibraryPath(project, javaHome, fc);
        return fc;
    }
    
    public static FileCollection systemLibraryPath(Project project, JavaCompile compileTask) {
        return systemLibraryPath(project, findJavaHome(compileTask));
    }
    
    public static FileCollection systemLibraryPath(Project project, Provider<JavaLauncher> launcher) {
        return systemLibraryPath(project, findJavaHome(launcher));
    }
    
    public static FileCollection fullLibraryPath(Project project, JavaCompile compileTask) {
        ConfigurableFileCollection fc = project.files();
        fc.from(compileTask.getClasspath());
        addSystemLibraryPath(project, findJavaHome(compileTask), fc);
        return fc;
    }
    
    private static void addSystemLibraryPath(Project project, Path javaHome, ConfigurableFileCollection libraryPath) {
        ConfigurableFileTree cpTreeJre = project.fileTree(javaHome.resolve("jre").resolve("lib"));
        cpTreeJre.include("*.jar");

        ConfigurableFileTree cpTreeJdk = project.fileTree(javaHome.resolve("lib"));
        cpTreeJdk.include("*.jar");

        ConfigurableFileTree mpTree = project.fileTree(javaHome.resolve("jmods"));
        mpTree.include("*.jmod");

        if (mpTree.isEmpty()) {
            libraryPath.from(cpTreeJre, cpTreeJdk);
        } else {
            // Exclude jars when modules are found as JDT will fail
            // when adding these together with jmods.
            libraryPath.from(cpTreeJre, mpTree);
        }
    }
}
