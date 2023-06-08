package org.moddingx.modgradle.plugins.sourcejar;

import org.apache.commons.io.IOUtils;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.plugins.coremods.CoreModsPlugin;
import org.moddingx.modgradle.util.java.JavaEnv;
import org.moddingx.modgradle.util.StringUtil;

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

public abstract class MergeJarWithSourcesTask extends AbstractArchiveTask {

    public MergeJarWithSourcesTask() {
        this.getArchiveBaseName().convention(this.getProject().provider(this.getProject()::getName));
        this.getArchiveVersion().convention(this.getProject().provider(() -> this.getProject().getVersion().toString()));
        this.getArchiveClassifier().convention(this.getProject().provider(() -> "sources"));
        this.getArchiveExtension().convention(this.getProject().provider(() -> "jar"));
        this.getOutputs().upToDateWhen(t -> false);
        // We need dummy sources, or it will always skip with NO-SOURCE
        this.from(this.getBase(), this.getSources());
        this.getCoreModSources().convention(JavaEnv.getJavaExtension(this.getProject()).map(ext -> {
            try {
                return ext.getSourceSets().getByName("coremods").getResources().getSourceDirectories();
            } catch (UnknownDomainObjectException e) {
                //noinspection ConstantConditions
                return null;
            }
        }));
    }
    
    @InputFile
    public abstract RegularFileProperty getBase();

    @InputFile
    public abstract RegularFileProperty getSources();

    @Optional
    @InputFiles
    public abstract Property<FileCollection> getCoreModSources();

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
        this.processJar(out, this.getBase().getAsFile().get().toPath(), dirs, false);
        this.processJar(out, this.getSources().getAsFile().get().toPath(), dirs, true);
        if (this.getCoreModSources().isPresent()) {
            for (File srcDir : this.getCoreModSources().get().getFiles()) {
                for (Path loc : CoreModsPlugin.getRelativeCoreModPaths(srcDir.toPath())) {
                    out.putNextEntry(new ZipEntry(loc.normalize().toString()));
                    Files.copy(srcDir.toPath().resolve(loc), out);
                    out.closeEntry();
                }
            }
        }
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
