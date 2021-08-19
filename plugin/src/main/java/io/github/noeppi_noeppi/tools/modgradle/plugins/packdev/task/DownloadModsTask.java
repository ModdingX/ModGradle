package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task;

import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class DownloadModsTask extends DefaultTask {

    private final List<CurseFile> files;
    
    private final RegularFileProperty modList = this.getProject().getObjects().fileProperty();
    private final DirectoryProperty output = this.getProject().getObjects().directoryProperty();

    @Inject
    public DownloadModsTask(List<CurseFile> files) {
        this.files = files;
        this.modList.convention(new DefaultProvider<>(() -> (RegularFile) () -> this.getProject().file("modlist.json")));
        this.output.set(this.getProject().file("build/mods"));
    }

    @InputFile
    public RegularFile getModList() {
        return this.modList.get();
    }
    
    public void setModList(RegularFile modList) {
        this.modList.set(modList);
    }
    
    public void setModList(File modList) {
        this.modList.set(modList);
    }
    
    @OutputDirectories
    public Directory getOutput() {
        return this.output.get();
    }

    public void setOutput(Directory output) {
        this.output.set(output);
    }

    public void setOutput(File outputDir) {
        this.output.set(outputDir);
    }

    @TaskAction
    public void downloadMods(InputChanges changes) throws IOException {
        PathUtils.deleteDirectory(this.getOutput().getAsFile().toPath());
        for (CurseFile file : this.files) {
            URL url = file.downloadUrl();
            String filePath = url.getPath();
            String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf('/') + 1) : filePath;
            Path target = this.getOutput().getAsFile().toPath().resolve(file.side().id).resolve(fileName);
            if (!Files.exists(target.getParent())) Files.createDirectories(target.getParent());
            InputStream in = url.openStream();
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            in.close();
        }
    }
}
