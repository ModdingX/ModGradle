package org.moddingx.modgradle.util.java;

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
import java.util.stream.Stream;

public class JavaHelper {
    
    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "non-sealed", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "permits", "record", "sealed",
            "var", "yield", "true", "false", "null", "_"
    );
    
    public static boolean isKeyword(String str) {
        return KEYWORDS.contains(str);
    }
    
    public static Set<String> findPackages(List<Path> sourceDirs) throws IOException {
        Set<String> packages = new HashSet<>();

        for (Path source : sourceDirs) {
            if (Files.isDirectory(source.toAbsolutePath().normalize())) {
                try (Stream<Path> paths = Files.walk(source)) {
                    //noinspection CodeBlock2Expr
                    paths.filter((path) -> {
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
        
        if (mpTree.isEmpty()) {
            libraryPath.from(cpTreeJre, cpTreeJdk);
        } else {
            // Exclude jars when modules are found as JDT will fail
            // when adding these together with jmods.
            libraryPath.from(cpTreeJre, mpTree);
        }
    }
}
