package io.github.noeppi_noeppi.tools.modgradle.util;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Manifest;

public class JarUtil {
    
    @Nullable
    public static String mainClass(File jarFile) throws IOException {
        return mainClass(jarFile.toPath());
    }
    
    @Nullable
    public static String mainClass(Path jarFile) throws IOException {
        FileSystem fs = IOUtil.getFileSystem(jarFile.toAbsolutePath().normalize().toUri());
        
        Path manifestPath = fs.getPath("/META-INF/MANIFEST.MF");
        if (Files.isRegularFile(manifestPath)) {
            try (InputStream in = Files.newInputStream(manifestPath)) {
                Manifest manifest = new Manifest(in);
                if (manifest.getMainAttributes().containsKey("Main-Class")) {
                    return manifest.getMainAttributes().get("Main-Class").toString().strip();
                }
            }
        }
        
        Path modulePath = fs.getPath("/module-info.class");
        if (Files.isRegularFile(modulePath)) {
            try (InputStream in = Files.newInputStream(modulePath)) {
                ModuleDescriptor module = ModuleDescriptor.read(in);
                if (module.mainClass().isPresent()) {
                    return module.mainClass().get();
                }
            }
        }
        
        return null;
    }
}
