package io.github.noeppi_noeppi.tools.modgradle.plugins.coremods;

import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.util.FSUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;
import org.gradle.work.InputChanges;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PackCoreModsTask extends DefaultTask {

    private final RegularFileProperty coreModTypes = this.getProject().getObjects().fileProperty();
    private final DirectoryProperty sourceDir = this.getProject().getObjects().directoryProperty();
    private final DirectoryProperty targetDir = this.getProject().getObjects().directoryProperty();

    public PackCoreModsTask() {
        this.targetDir.set(this.getProject().file("build").toPath().resolve("coremods").resolve("js").toFile());
    }

    @InputFile
    public RegularFile getCoreModTypes() {
        return this.coreModTypes.get();
    }

    public void setCoreModTypes(RegularFile coreModTypes) {
        this.coreModTypes.set(coreModTypes);
    }

    public void setCoreModTypes(File coreModTypes) {
        this.coreModTypes.set(coreModTypes);
    }
    
    @InputDirectory
    public Directory getSourceDir() {
        return this.sourceDir.get();
    }

    public void setSourceDir(Directory sourceDir) {
        this.sourceDir.set(sourceDir);
    }

    public void setSourceDir(File targetDir) {
        this.sourceDir.set(targetDir);
    }

    @OutputDirectory
    public Directory getTargetDir() {
        return this.targetDir.get();
    }

    public void setTargetDir(Directory targetDir) {
        this.targetDir.set(targetDir);
    }

    public void setTargetDir(File targetDir) {
        this.targetDir.set(targetDir);
    }

    @TaskAction
    public void packCoreMods(InputChanges changes) throws IOException {
        Path source = this.getSourceDir().getAsFile().toPath().toAbsolutePath().normalize();
        List<Path> coreMods = Files.walk(source)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".js"))
                .map(p -> source.relativize(p.toAbsolutePath()))
                .toList();
        
        Path target = this.getTargetDir().getAsFile().toPath();
        if (!Files.exists(target)) Files.createDirectories(target);
        
        String baseJs;
        try (FileSystem fs = FSUtil.getFileSystem(URI.create("jar:" + this.getCoreModTypes().getAsFile().toPath().toUri()))) {
            baseJs = Files.readString(fs.getPath("coremods.js")) + "\n";
        }

        JsonObject json = new JsonObject();
        for (Path loc : coreMods) {
            Path dest = target.resolve(loc);
            if (!Files.isDirectory(dest.getParent())) Files.createDirectories(dest.getParent());
            Writer writer = Files.newBufferedWriter(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            writer.write(baseJs + "\n");
            writer.write(Files.readString(source.resolve(loc)) + "\n");
            writer.close();
            
            String name = loc.getFileName() == null ? "" : loc.getFileName().toString();
            if (name.endsWith(".js")) name = name.substring(0, name.length() - 3);
            if (json.has(name)) throw new IllegalStateException("Duplicate CoreMod name: " + name);
            json.addProperty(name, IntStream.range(0, loc.getNameCount()).mapToObj(loc::getName).map(Path::toString).collect(Collectors.joining("/")));
        }
        
        Files.writeString(target.resolve("META-INF").resolve("coremods.json"), ModGradle.GSON.toJson(json) + "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
