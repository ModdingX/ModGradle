package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public abstract class ApplyRenameCommentsTask extends JarExec {

    private final DirectoryProperty sources = this.getProject().getObjects().directoryProperty();

    public ApplyRenameCommentsTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("comments", "--sources", "{sources}");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
        this.getOutputs().upToDateWhen(t -> false);
    }
    
    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        return this.replaceArgs(args, Map.of(
                "{sources}", this.getSources().getAsFile().toPath().toAbsolutePath().normalize().toString()
        ), null);
    }
    
    @InputDirectory
    public Directory getSources() {
        return this.sources.get();
    }

    public void setSources(Directory classes) {
        this.sources.set(classes);
    }
}
