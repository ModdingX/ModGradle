package io.github.noeppi_noeppi.tools.modgradle.util;

import com.google.common.collect.ImmutableSet;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ConfigurableFileTree;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaHelper {
    
    private static final Set<String> KEYWORDS = ImmutableSet.of(
            "abstract", "continue", "for", "new", "switch",
            "assert", "default", "goto", "package", "synchronized",
            "boolean", "do", "if", "private", "this",
            "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try",
            "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile",
            "const", "float", "native", "super", "while", "_"
    );
    
    public static boolean isKeyword(String str) {
        return KEYWORDS.contains(str);
    }
    
    public static Set<String> findPackages(List<Path> sourceDirs) throws IOException {
        Set<String> packages = new HashSet<>();

        for (Path source : sourceDirs) {
            if (Files.isDirectory(source.toAbsolutePath().normalize())) {
                //noinspection CodeBlock2Expr
                Files.walk(source).filter((path) -> {
                    if (Files.isDirectory(path) && !path.getFileName().toString().equals("META-INF")
                            && !JavaHelper.isKeyword(path.getFileName().toString())) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                            for (Path content : stream) {
                                if (Files.isRegularFile(content) && content.getFileName().toString().endsWith(".java"))
                                    return true;
                            }
                            return false;
                        } catch (IOException e) {
                            throw new IllegalStateException("Could not read directory " + source.relativize(path).normalize());
                        }
                    } else {
                        return false;
                    }
                }).forEach((path) -> {
                    packages.add(source.relativize(path).normalize().toString().replace('/', '.'));
                });
            }
        }
        
        return Collections.unmodifiableSet(packages);
    }

    public static void addBuiltinLibraries(Project project, Path javaHome, ConfigurableFileCollection libraryPath) {
        ConfigurableFileTree cpTreeJre = project.fileTree(javaHome.resolve("jre").resolve("lib"));
        cpTreeJre.include("*.jar");

        ConfigurableFileTree cpTreeJdk = project.fileTree(javaHome.resolve("lib"));
        cpTreeJdk.include("*.jar");

        ConfigurableFileTree mpTree = project.fileTree(javaHome.resolve("jmods"));
        mpTree.include("*.jmod");

        libraryPath.from(cpTreeJre, cpTreeJdk, mpTree);
    }
}
