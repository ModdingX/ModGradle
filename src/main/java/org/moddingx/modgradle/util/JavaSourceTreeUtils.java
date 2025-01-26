package org.moddingx.modgradle.util;

import org.apache.commons.io.function.IOSupplier;

import javax.lang.model.SourceVersion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class JavaSourceTreeUtils {

    public static Set<String> findPackages(List<Path> sourceDirs) throws IOException {
        Set<String> packages = new HashSet<>();
        try {
            for (Path source : sourceDirs) {
                if (Files.isDirectory(source)) {
                    try (Stream<Path> paths = Files.walk(source)) {
                        paths.filter((path) -> unchecked(() -> isJavaPackageDirectory(path))).forEach((path) -> packages.add(packageName(source, path)));
                    }
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return Collections.unmodifiableSet(packages);
    }
    
    private static <T> T unchecked(IOSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    private static boolean isJavaPackageDirectory(Path path) throws IOException {
        return Files.isDirectory(path) && !path.getFileName().toString().equals("META-INF") && !SourceVersion.isKeyword(path.getFileName().toString()) && containsJavaSourceFile(path);
    }
    
    private static boolean containsJavaSourceFile(Path path) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path content : stream) {
                if (Files.isRegularFile(content) && content.getFileName().toString().endsWith(".java")) {
                    return true;
                }
            }
            return false;
        }
    }
    
    private static String packageName(Path root, Path path) {
        Path relPackagePath = root.relativize(path);
        // Sanity check for cursed filesystem implementations.
        if (!relPackagePath.normalize().isAbsolute()) relPackagePath = relPackagePath.normalize();
        return IntStream.range(0, relPackagePath.getNameCount()).mapToObj(relPackagePath::getName).map(Path::toString).collect(Collectors.joining("."));
    }
}
