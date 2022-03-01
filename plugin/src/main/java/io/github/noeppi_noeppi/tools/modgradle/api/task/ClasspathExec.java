package io.github.noeppi_noeppi.tools.modgradle.api.task;

import net.minecraftforge.gradle.common.tasks.JarExec;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.work.InputChanges;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A task that executes some java code. Unlike {@link JarExec} this does not require a fatjar.
 * It will instead resolve a configuration to include dependencies.
 */
public abstract class ClasspathExec extends DefaultTask {
    
    public ClasspathExec() {
        this.getJavaLauncher().convention(this.getProject().provider(() -> this.getJavaToolchainService().launcherFor(spec -> spec.getLanguageVersion().set(this.getJavaVersion().map(JavaLanguageVersion::of))).get()));
        this.getLogFile().set(this.getProject().file("build").toPath().resolve(this.getName()).resolve("log.txt").toFile());
    }

    /**
     * The {@link Dependency} to load.
     */
    @Input
    public abstract ListProperty<String> getArgs();
    
    public void tool(Object dep) {
        this.getTool().set(this.getProject().getDependencies().create(dep));
    }
    
    /**
     * The {@link Dependency} to load.
     */
    @Input
    public abstract Property<Dependency> getTool();
    
    /**
     * The file to write the log.
     */
    @OutputFile
    public abstract RegularFileProperty getLogFile();

    @Inject
    protected abstract JavaToolchainService getJavaToolchainService();
    
    /**
     * The java version to use.
     */
    @Input
    public abstract Property<Integer> getJavaVersion();
    
    /**
     * The java launcher to use.
     */
    @Input
    public abstract Property<JavaLauncher> getJavaLauncher();

    protected List<String> processArgs(List<String> args) {
        return args;
    }
    
    @TaskAction
    public void exec(InputChanges changes) throws IOException {
        // TODO
    }
}
