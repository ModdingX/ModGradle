package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task.multimc;

import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.MultiMCExtension;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackSettings;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class UpdateMultiMCTask extends MultiMCTask {
    
    @Inject
    public UpdateMultiMCTask(PackSettings settings, MultiMCExtension ext, List<CurseFile> files) {
        super(settings, ext, files);
    }

    @TaskAction
    public void updateInstance(InputChanges changes) throws IOException {
        Path target = this.ext.getInstancePath();
        if (!Files.isDirectory(target.resolve("minecraft"))) {
            Files.createDirectories(target.resolve("minecraft"));
        }
        
        System.err.println("Update data for MultiMc");
        
        Map<Integer, Integer> cache = CurseFile.readCache(target.resolve("minecraft").resolve("mods").resolve("cache.json"));
        boolean needsDownload = this.files.size() != cache.size() || this.files.stream().anyMatch(f -> !cache.containsKey(f.projectId()) || cache.get(f.projectId()) != f.fileId());
        
        if (needsDownload && Files.exists(target.resolve("minecraft").resolve("mods"))) PathUtils.deleteDirectory(target.resolve("minecraft").resolve("mods"));
        
        System.err.println("Copy overrides");
        PathUtils.copyDirectory(this.getProject().file("data/" + Side.COMMON.id).toPath(), target.resolve("minecraft"), StandardCopyOption.REPLACE_EXISTING);
        PathUtils.copyDirectory(this.getProject().file("data/" + Side.CLIENT.id).toPath(), target.resolve("minecraft"), StandardCopyOption.REPLACE_EXISTING);

        if (needsDownload) {
            System.err.println("Downloading mods");
            if (!Files.isDirectory(target.resolve("minecraft").resolve("mods"))) {
                Files.createDirectories(target.resolve("minecraft").resolve("mods"));
            }
            for (CurseFile file : this.files) {
                String fname = file.fileName();
                System.err.println("Downloading " + fname);
                Path modTarget = target.resolve("minecraft").resolve("mods").resolve(fname);
                InputStream in = file.downloadUrl().openStream();
                Files.copy(in, modTarget, StandardCopyOption.REPLACE_EXISTING);
                in.close();
            }
            CurseFile.writeCache(target.resolve("minecraft").resolve("mods").resolve("cache.json"), this.files);
        }
    }
}
