package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public abstract class ApplyRenameCommentsTask extends JarExec {
    
    public ApplyRenameCommentsTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("comments", "--sources", "{sources}");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
        this.getOutputs().upToDateWhen(t -> false);
    }

    @InputDirectory
    public abstract DirectoryProperty getSources();

    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        return this.replaceArgs(args, Map.of(
                "{sources}", this.getSources().get().getAsFile().toPath().toAbsolutePath().normalize().toString()
        ), Map.of());
    }
}