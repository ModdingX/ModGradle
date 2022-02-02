package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public abstract class StageLocalTask extends JarExec {
    
    public StageLocalTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("apply", "--sources", "{sources}", "--rename", "{rename}", "--comments");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
        this.getOutputs().upToDateWhen(t -> false);
    }
    
    @InputFile
    public abstract RegularFileProperty getRenameMap();

    @InputFiles
    public abstract Property<FileCollection> getSources();

    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        return this.replaceArgs(args, Map.of(
                "{sources}", this.getSources().get().getAsPath(),
                "{rename}", this.getRenameMap().getAsFile().get().toPath().toAbsolutePath().normalize().toString()
        ), Map.of());
    }
}
