package io.github.noeppi_noeppi.tools.modgradle.util.task;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public abstract class CreateRenameCommentsTask extends JarExec {

    private final DirectoryProperty sources = this.getProject().getObjects().directoryProperty();
    private final RegularFileProperty renameMap = this.getProject().getObjects().fileProperty();

    public CreateRenameCommentsTask() {
        this.getTool().set(ModGradle.SOURCE_TRANSFORM);
        this.getArgs().addAll("apply", "--sources", "{sources}", "--rename", "{rename}", "--comments");
        this.setRuntimeJavaVersion(ModGradle.TARGET_JAVA);
        this.getOutputs().upToDateWhen(t -> false);
    }
    
    @Nonnull
    @Override
    protected List<String> filterArgs(@Nonnull List<String> args) {
        return this.replaceArgs(args, Map.of(
                "{sources}", this.getSources().getAsFile().toPath().toAbsolutePath().normalize().toString(),
                "{rename}", this.getRenameMap().getAsFile().toPath().toAbsolutePath().normalize().toString()
        ), null);
    }
    
    @InputDirectory
    public Directory getSources() {
        return this.sources.get();
    }

    public void setSources(Directory classes) {
        this.sources.set(classes);
    }
    
    @InputFile
    public RegularFile getRenameMap() {
        return this.renameMap.get();
    }

    public void setRenameMap(RegularFile renameMap) {
        this.renameMap.set(renameMap);
    }
}
