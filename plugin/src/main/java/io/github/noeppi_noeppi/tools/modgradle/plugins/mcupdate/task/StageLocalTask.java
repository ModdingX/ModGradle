package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.api.task.ClasspathExec;
import io.github.noeppi_noeppi.tools.modgradle.util.ArgumentUtil;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public abstract class StageLocalTask extends ClasspathExec {
    
    public StageLocalTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("rename", "--sources", "{sources}", "--rename", "{rename}", "--comments");
        this.getOutputs().upToDateWhen(t -> false);
    }
    
    @InputFile
    public abstract RegularFileProperty getRenameMap();

    @InputFiles
    public abstract Property<FileCollection> getSources();

    @Override
    protected List<String> processArgs(List<String> args) {
        return ArgumentUtil.replaceArgs(args, Map.of(
                "sources", List.of(this.getSources()),
                "rename", List.of(this.getRenameMap())
        ));
    }
}
