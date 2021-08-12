package io.github.noeppi_noeppi.tools.modgradle.plugins.sourcejar;

import io.github.noeppi_noeppi.tools.modgradle.util.StringUtil;
import org.apache.commons.io.IOUtils;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.work.InputChanges;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MergeJarWithSourcesTask extends AbstractArchiveTask {
    
    private final Property<RegularFile> base = this.getProject().getObjects().fileProperty();
    private final Property<RegularFile> sources = this.getProject().getObjects().fileProperty();

    public MergeJarWithSourcesTask() {
        this.getArchiveBaseName().convention(new DefaultProvider<>(this.getProject()::getName));
        this.getArchiveVersion().convention(new DefaultProvider<>(() -> this.getProject().getVersion().toString()));
        this.getArchiveClassifier().convention(new DefaultProvider<>(() -> "sources"));
        this.getArchiveExtension().convention(new DefaultProvider<>(() -> "jar"));
        this.getOutputs().upToDateWhen(t -> false);
        // We need dummy sources, or it will always skip with NO-SOURCE
        this.from(this.base, this.sources);
    }
    
    @InputFile
    public RegularFile getBase() {
        return this.base.get();
    }

    public void setBase(RegularFile base) {
        this.base.set(base);
    }
    
    public void setBase(File base) {
        this.base.set(() -> base);
    }

    @InputFile
    public RegularFile getSources() {
        return this.sources.get();
    }

    public void setSources(RegularFile sources) {
        this.sources.set(sources);
    }
    
    public void setSources(File sources) {
        this.sources.set(() -> sources);
    }

    @Nonnull
    @Override
    protected CopyAction createCopyAction() {
        // Just do nothing
        return copy -> () -> true;
    }

    @TaskAction
    protected void mergeJars(InputChanges inputs) throws IOException {
        ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(this.getArchiveFile().get().getAsFile().toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
        Set<String> dirs = new HashSet<>();
        this.processJar(out, this.getBase().getAsFile().toPath(), dirs, false);
        this.processJar(out, this.getSources().getAsFile().toPath(), dirs, true);
        out.close();
    }
    
    private void processJar(ZipOutputStream out, Path jarFile, Set<String> dirs, boolean sources) throws IOException {
        ZipInputStream zin = new ZipInputStream(Files.newInputStream(jarFile));
        for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
            String name = entry.getName().substring(StringUtil.indexWhere(entry.getName(), c -> c != '/'));
            if (entry.isDirectory()) {
                if (!dirs.contains(name)) {
                    dirs.add(name);
                    out.putNextEntry(new ZipEntry(name));
                    out.closeEntry();
                }
            } else if (sources && name.toLowerCase(Locale.ROOT).endsWith(".java")) {
                out.putNextEntry(new ZipEntry(name));
                IOUtils.copy(zin, out);
                out.closeEntry();
            } else if (!sources && !name.toLowerCase(Locale.ROOT).endsWith(".java") && !name.toLowerCase(Locale.ROOT).endsWith(".class")) {
                out.putNextEntry(new ZipEntry(name));
                IOUtils.copy(zin, out);
                out.closeEntry();
            }
        }
        zin.close();
    }
}
