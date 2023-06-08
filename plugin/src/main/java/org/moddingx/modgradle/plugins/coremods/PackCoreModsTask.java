package org.moddingx.modgradle.plugins.coremods;

import com.google.gson.JsonObject;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.util.io.IOUtil;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class PackCoreModsTask extends DefaultTask {
    
    public PackCoreModsTask() {
        this.getTargetDir().set(this.getProject().file("build").toPath().resolve("coremods").resolve("js").toFile());
    }

    @InputFile
    public abstract RegularFileProperty getCoreModTypes();

    @InputDirectory
    public abstract DirectoryProperty getSourceDir();

    @OutputDirectory
    public abstract DirectoryProperty getTargetDir();

    @TaskAction
    public void packCoreMods(InputChanges inputs) throws IOException {
        Path source = this.getSourceDir().get().getAsFile().toPath().toAbsolutePath().normalize();
        
        List<Path> coreMods;
        try (Stream<Path> paths = Files.walk(source)) {
            coreMods = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".js"))
                .map(p -> source.relativize(p.toAbsolutePath()))
                .toList();
        }
        
        Path target = this.getTargetDir().get().getAsFile().toPath();
        Files.createDirectories(target);
        
        String baseJs;
        try (FileSystem fs = IOUtil.getFileSystem(URI.create("jar:" + this.getCoreModTypes().get().getAsFile().toPath().toUri()))) {
            baseJs = Files.readString(fs.getPath("coremods.js"), StandardCharsets.UTF_8) + "\n";
        }

        JsonObject json = new JsonObject();
        for (Path loc : coreMods.stream().sorted(Comparator.nullsFirst(Comparator.comparing(Path::getFileName))).toList()) {
            Path dest = target.resolve(loc);
            PathUtils.createParentDirectories(dest);
            Writer writer = Files.newBufferedWriter(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            writer.write(baseJs + "\n");
            writer.write(Files.readString(source.resolve(loc), StandardCharsets.UTF_8) + "\n");
            writer.close();
            
            String name = loc.getFileName() == null ? "" : loc.getFileName().toString();
            if (name.endsWith(".js")) name = name.substring(0, name.length() - 3);
            if (json.has(name)) throw new IllegalStateException("Duplicate CoreMod name: " + name);
            json.addProperty(name, IntStream.range(0, loc.getNameCount()).mapToObj(loc::getName).map(Path::toString).collect(Collectors.joining("/")));
        }
        
        Files.createDirectories(target.resolve("META-INF"));
        Files.writeString(target.resolve("META-INF").resolve("coremods.json"), ModGradle.GSON.toJson(json) + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
