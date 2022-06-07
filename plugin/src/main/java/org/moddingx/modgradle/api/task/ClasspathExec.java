package org.moddingx.modgradle.api.task;

import net.minecraftforge.gradle.common.tasks.JarExec;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.util.ConfigurationDownloader;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * A task that executes some java code. Unlike {@link JarExec} this does not require a fatjar.
 * It will instead resolve a configuration to include dependencies.
 */
public abstract class ClasspathExec extends DefaultTask {
    
    public ClasspathExec() {
        this.getJavaVersion().convention(ModGradle.TARGET_JAVA);
        this.getJavaLauncher().convention(this.getProject().provider(() -> this.getJavaToolchainService().launcherFor(spec -> spec.getLanguageVersion().set(this.getJavaVersion().map(JavaLanguageVersion::of))).get()));
        this.getWorkingDirectory().set(this.getProject().file("build").toPath().resolve(this.getName()).toFile());
        this.getLogFile().set(this.getProject().file("build").toPath().resolve(this.getName()).resolve("log.txt").toFile());
    }

    /**
     * The {@link Dependency} to load.
     */
    @Input
    public abstract ListProperty<String> getArgs();
    
    /**
     * The dependency to load.
     */
    @Input
    public abstract Property<String> getTool();

    /**
     * The directory to start the process in.
     */
    @OutputFile
    public abstract DirectoryProperty getWorkingDirectory();
    
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
    @Nested
    public abstract Property<JavaLauncher> getJavaLauncher();

    protected List<String> processArgs(List<String> args) {
        return args;
    }
    
    @TaskAction
    public void exec(InputChanges changes) throws IOException {
        ConfigurationDownloader.Executable executable = ConfigurationDownloader.executable(this.getProject(), this.getTool().get());
        if (executable == null) throw new IllegalStateException("Could not resolve tool: " + this.getTool().get());
        List<String> arguments = this.processArgs(this.getArgs().get());
        Path logFile = this.getLogFile().get().getAsFile().toPath().toAbsolutePath().normalize();
        PathUtils.createParentDirectories(logFile);
        try (PrintStream out = new PrintStream(Files.newOutputStream(logFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            String java = this.getJavaLauncher().get().getExecutablePath().getAsFile().toPath().toAbsolutePath().normalize().toString();
            out.println("Java: " + java);
            out.println("Classpath: " + executable.classpath().getAsPath());
            out.println("Working Directory: " + this.getWorkingDirectory().get().getAsFile().toPath().toAbsolutePath().normalize());
            out.println("Main Class: " + executable.mainClass());
            out.println("Arguments: " + String.join(" ", arguments));
            out.println("\n");
            this.getProject().javaexec(spec -> {
                spec.setExecutable(java);
                spec.setClasspath(executable.classpath());
                spec.setWorkingDir(this.getWorkingDirectory().get().getAsFile());
                spec.getMainClass().set(executable.mainClass());
                spec.setArgs(arguments);
                
                spec.setStandardInput(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        return -1;
                    }
                });
                
                spec.setStandardOutput(new OutputStream() {
                    
                    @Override
                    public void write(int data) throws IOException {
                        out.write(data);
                    }

                    @Override
                    public void write(@Nonnull byte[] data) throws IOException {
                        out.write(data);
                    }

                    @Override
                    public void write(@Nonnull byte[] data, int off, int len) throws IOException {
                        out.write(data, off, len);
                    }
                });
                
                spec.setErrorOutput(new OutputStream() {
                    
                    @Override
                    public void write(int data) throws IOException {
                        System.err.write(data);
                        out.write(data);
                    }

                    @Override
                    public void write(@Nonnull byte[] data) throws IOException {
                        System.err.write(data);
                        out.write(data);
                    }

                    @Override
                    public void write(@Nonnull byte[] data, int off, int len) throws IOException {
                        System.err.write(data, off, len);
                        out.write(data, off, len);
                    }
                });
            }).rethrowFailure().assertNormalExitValue();
        }
    }
}
