package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.api.task.ClasspathExec;
import io.github.noeppi_noeppi.tools.modgradle.util.ArgumentUtil;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public abstract class ApplyRenameCommentsTask extends ClasspathExec {
    
    public ApplyRenameCommentsTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("comments", "--sources", "{sources}");
        this.getOutputs().upToDateWhen(t -> false);
    }

    @InputDirectory
    public abstract DirectoryProperty getSources();

    @Override
    protected List<String> processArgs(List<String> args) {
        return ArgumentUtil.replaceArgs(args, Map.of(
                "sources", List.of(this.getSources().get())
        ));
    }
}
