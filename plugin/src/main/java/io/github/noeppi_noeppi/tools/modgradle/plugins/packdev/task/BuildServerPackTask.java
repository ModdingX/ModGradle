package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task;

import io.github.noeppi_noeppi.tools.modgradle.api.Versioning;
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
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class BuildServerPackTask extends BuildTargetTask {

    @Inject
    public BuildServerPackTask(PackSettings settings, List<CurseFile> files, String edition) {
        super(settings, files, edition);
    }

    @Override
    protected void generate(Path target) throws IOException {
        FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + target.toUri()), Map.of(
                "create", String.valueOf(!Files.exists(target))
        ));
        for (Path src : this.getOverridePaths(Side.SERVER)) {
            PathUtils.copyDirectory(src, fs.getPath(""));
        }
                
        InputStream installScript = PackDevPlugin.class.getResourceAsStream("/" + PackDevPlugin.class.getPackage().getName().replace('.', '/') + "/install_server.py");
        if (installScript == null) throw new IllegalStateException("Can't build server pack: Install script not found in ModGradle.");
        Files.copy(installScript, fs.getPath("install.py"));
        installScript.close();
        try {
            HashSet<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(fs.getPath("install.py")));
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(fs.getPath("install.py"), perms);
        } catch (Exception e) {
            //
        }
        
        InputStream dockerFile = PackDevPlugin.class.getResourceAsStream("/" + PackDevPlugin.class.getPackage().getName().replace('.', '/') + "/Dockerfile");
        if (dockerFile == null) throw new IllegalStateException("Can't build server pack: Dockerfile not found in ModGradle.");
        CopyUtil.copyFile(dockerFile, fs.getPath("Dockerfile"), Map.of(
                "jdk", Integer.toString(Versioning.getJavaVersion(this.settings.minecraft()))
        ), true);
        dockerFile.close();
        
        this.generateServerInfo(fs.getPath("server.txt"));
        fs.close();
    }
    
    private void generateServerInfo(Path target) throws IOException {
        Writer writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE_NEW);
        writer.write(this.settings.minecraft() + "/" + this.settings.forge() + "\n");
        for (CurseFile file : this.files.stream().sorted(Comparator.comparing(CurseFile::projectId)).toList()) {
            if (file.side().server) {
                writer.write(file.projectId() + "/" + file.fileId() + "\n");
            }
        }
        writer.close();
    }
}
