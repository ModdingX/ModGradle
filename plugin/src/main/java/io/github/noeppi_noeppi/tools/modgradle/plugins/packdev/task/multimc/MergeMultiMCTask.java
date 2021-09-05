package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task.multimc;

import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.MultiMCExtension;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackSettings;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class MergeMultiMCTask extends MultiMCTask {
    
    @Inject
    public MergeMultiMCTask(PackSettings settings, MultiMCExtension ext, List<CurseFile> files) {
        super(settings, ext, files);
    }

    @TaskAction
    public void updateInstance(InputChanges changes) throws IOException {
        Path target = this.ext.getInstancePath();
        if (!Files.isDirectory(target.resolve("minecraft"))) {
            Files.createDirectories(target.resolve("minecraft"));
        }

        System.err.println("Copying modified overrides");
        this.copyBack(this.getProject().file("data/" + Side.CLIENT.id).toPath(), target.resolve("minecraft"));
        this.copyBack(this.getProject().file("data/" + Side.COMMON.id).toPath(), target.resolve("minecraft"));
    }
    
    private void copyBack(Path overrides, Path minecraft) throws IOException {
        List<Path> entries = Files.walk(overrides.toAbsolutePath())
                .map(Path::toAbsolutePath)
                .filter(Files::isRegularFile)
                .map(overrides::relativize)
                .filter(p -> !p.startsWith("mods"))
                .filter(p -> Files.isRegularFile(minecraft.resolve(p)))
                .toList();
        for (Path p : entries) {
            Files.copy(minecraft.resolve(p), overrides.resolve(p), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
