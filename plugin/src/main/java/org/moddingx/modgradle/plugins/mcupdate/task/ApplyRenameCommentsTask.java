package org.moddingx.modgradle.plugins.mcupdate.task;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.api.task.ClasspathExec;
import org.moddingx.modgradle.util.ArgumentUtil;

import java.util.List;
import java.util.Map;

public abstract class ApplyRenameCommentsTask extends ClasspathExec {
    
    public ApplyRenameCommentsTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("rename", "--sources", "{sources}", "--comments");
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
