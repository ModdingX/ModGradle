package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.task.multimc;

import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.CurseFile;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.MultiMCExtension;
import io.github.noeppi_noeppi.tools.modgradle.plugins.packdev.PackSettings;
import io.github.noeppi_noeppi.tools.modgradle.util.IOUtil;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.MultiMCApi;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.work.InputChanges;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;

public class SetupMultiMCTask extends MultiMCTask {
    
    private final Task updateTask;
    private final Task mergeTask;
    
    @Inject
    public SetupMultiMCTask(PackSettings settings, MultiMCExtension ext, List<CurseFile> files, Task updateTask, Task mergeTask) {
        super(settings, ext, files);
        this.updateTask = updateTask;
        this.mergeTask = mergeTask;
    }

    @TaskAction
    public void generateInstance(InputChanges changes) throws IOException {
        Path target = this.ext.getInstancePath(this.getProject());
        if (!Files.isDirectory(target)) {
            Files.createDirectories(target);
        }
        Writer instanceCfg = Files.newBufferedWriter(target.resolve("instance.cfg"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        this.generateInstanceConfig(instanceCfg);
        instanceCfg.close();

        JsonObject json = MultiMCApi.buildForgePack(this.settings.forge());
        Writer mmcPack = Files.newBufferedWriter(target.resolve("mmc-pack.json"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        mmcPack.write(ModGradle.GSON.toJson(json) + "\n");
        mmcPack.close();
        
        if (!Files.exists(target.resolve("minecraft"))) {
            Files.createDirectories(target.resolve("minecraft"));
        }
    }
    
    private void generateInstanceConfig(Writer writer) throws IOException {
        writer.write("ForgeVersion=\n");
        writer.write("InstanceType=OneSix\n");
        writer.write("IntendedVersion=\n");
        writer.write("LWJGLVersion=\n");
        writer.write("LiteloaderVersion=\n");
        writer.write("LogPrePostOutput=true\n");
        writer.write("MCLaunchMethod=LauncherPart\n");
        writer.write("\n");
        writer.write("OverrideConsole=false\n");
        writer.write("OverrideJavaArgs=false\n");
        writer.write("OverrideMCLaunchMethod=false\n");
        writer.write("OverrideMemory=false\n");
        writer.write("OverrideNativeWorkarounds=false\n");
        writer.write("OverrideWindow=false\n");
        writer.write("\n");
        writer.write("OverrideJavaLocation=true\n");
        JavaToolchainService service = this.getProject().getExtensions().getByType(JavaToolchainService.class);
        File jreFile = service.launcherFor(JavaEnv.getJavaExtension(this.getProject()).getToolchain()).get().getExecutablePath().getAsFile();
        writer.write("JavaPath=" + jreFile.toPath().toAbsolutePath().normalize() + "\n");
        writer.write("\n");
        writer.write("OverrideCommands=true\n");
        String gradleCommand;
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            gradleCommand = this.getProject().getRootProject().file("gradlew.bat").toPath().toAbsolutePath().normalize().toString();
        } else {
            gradleCommand = this.getProject().getRootProject().file("gradlew").toPath().toAbsolutePath().normalize().toString();
        }
        String gradleDir = this.getProject().getRootProject().file(".").toPath().toAbsolutePath().normalize().toString();
        writer.write("PreLaunchCommand=" + gradleCommand + " --stacktrace --project-dir " + IOUtil.quote(gradleDir) + " " + IOUtil.quote(this.updateTask.getPath()) + "\n");
        writer.write("PostExitCommand=" + gradleCommand + " --stacktrace --project-dir " + IOUtil.quote(gradleDir) + " " + IOUtil.quote(this.mergeTask.getPath()) + "\n");
        writer.write("\n");
        writer.write("name=" + this.getProject().getName() + "\n");
    }
}
