package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task.multimc;

import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.MultiMCExtension;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackSettings;
import org.gradle.api.DefaultTask;

import javax.inject.Inject;
import java.util.List;

public class MultiMCTask extends DefaultTask {

    protected final PackSettings settings;
    protected final MultiMCExtension ext;
    protected final List<CurseFile> files;

    @Inject
    public MultiMCTask(PackSettings settings, MultiMCExtension ext, List<CurseFile> files) {
        this.settings = settings;
        this.ext = ext;
        this.files = files.stream().filter(f -> f.side().client).toList();
        this.getOutputs().upToDateWhen(t -> false);
    }
}
