package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task;

import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackDevPlugin;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackSettings;
import io.github.noeppi_noeppi.tools.modgradle.util.Side;
import org.apache.commons.io.file.PathUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

public class BuildServerPackTask extends BuildTargetTask {

    @Inject
    public BuildServerPackTask(PackSettings settings, List<CurseFile> files) {
        super(settings, files);
    }

    @Override
    protected void generate(Path target) throws IOException {
        FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + target.toUri()), Map.of(
                "create", String.valueOf(!Files.exists(target))
        ));
        if (Files.exists(this.getProject().file("data/" + Side.COMMON.id).toPath())) PathUtils.copyDirectory(this.getProject().file("data/" + Side.COMMON.id).toPath(), fs.getPath(""));
        if (Files.exists(this.getProject().file("data/" + Side.SERVER.id).toPath())) PathUtils.copyDirectory(this.getProject().file("data/" + Side.SERVER.id).toPath(), fs.getPath(""));
        InputStream installScript = PackDevPlugin.class.getResourceAsStream("/" + PackDevPlugin.class.getPackage().getName().replace('.', '/') + "/install_server.py");
        if (installScript == null) throw new IllegalStateException("Can't build server pack: Install script not found in ModGradle.");
        Files.copy(installScript, fs.getPath("install.py"));
        installScript.close();
        this.generateServerInfo(fs.getPath("server.txt"));
        fs.close();
    }
    
    private void generateServerInfo(Path target) throws IOException {
        Writer writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE_NEW);
        writer.write(this.settings.minecraft() + "/" + this.settings.forge() + "\n");
        for (CurseFile file : this.files) {
            if (file.side().server) {
                writer.write(file.projectId() + "/" + file.fileId() + "\n");
            }
        }
        writer.close();
    }
}
