package io.github.noeppi_noeppi.tools.modgradle.plugins.coremods;

import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.util.IOUtil;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public void packCoreMods(InputChanges changes) throws IOException {
        Path source = this.getSourceDir().get().getAsFile().toPath().toAbsolutePath().normalize();
        List<Path> coreMods = Files.walk(source)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".js"))
                .map(p -> source.relativize(p.toAbsolutePath()))
                .toList();
        
        Path target = this.getTargetDir().get().getAsFile().toPath();
        Files.createDirectories(target);
        
        String baseJs;
        try (FileSystem fs = IOUtil.getFileSystem(URI.create("jar:" + this.getCoreModTypes().get().getAsFile().toPath().toUri()))) {
            baseJs = Files.readString(fs.getPath("coremods.js")) + "\n";
        }

        JsonObject json = new JsonObject();
        for (Path loc : coreMods) {
            Path dest = target.resolve(loc);
            PathUtils.createParentDirectories(dest);
            Writer writer = Files.newBufferedWriter(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            writer.write(baseJs + "\n");
            writer.write(Files.readString(source.resolve(loc)) + "\n");
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
