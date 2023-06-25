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
import org.moddingx.modgradle.util.io.zip.ZipBuilder;
import org.moddingx.modgradle.util.java.JavaEnv;
import org.moddingx.modgradle.util.StringUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
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
        ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(this.getArchiveFile().get().getAsFile().toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
        ZipBuilder zip = ZipBuilder.create(zipOut, this.isPreserveFileTimestamps(), this.isReproducibleFileOrder());
        Set<String> dirs = new HashSet<>();
        this.processJar(zip, this.getBase().getAsFile().get().toPath(), dirs, false);
        this.processJar(zip, this.getSources().getAsFile().get().toPath(), dirs, true);
        if (this.getCoreModSources().isPresent()) {
            for (File srcDir : this.getCoreModSources().get().getFiles()) {
                for (Path loc : CoreModsPlugin.getRelativeCoreModPaths(srcDir.toPath())) {
                    try (OutputStream out = zip.addEntry(loc.normalize().toString())) {
                        Files.copy(srcDir.toPath().resolve(loc), out);
                    }
                }
            }
        }
        zip.close();
        zipOut.close();
    }
    
    private void processJar(ZipBuilder zip, Path jarFile, Set<String> dirs, boolean sources) throws IOException {
        ZipInputStream zin = new ZipInputStream(Files.newInputStream(jarFile));
        for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
            String name = entry.getName().substring(StringUtil.indexWhere(entry.getName(), c -> c != '/'));
            if (entry.isDirectory()) {
                if (!dirs.contains(name)) {
                    dirs.add(name);
                    //noinspection EmptyTryBlock
                    try (OutputStream ignored = zip.addEntry(name, entry)) {}
                }
            } else if (sources && name.toLowerCase(Locale.ROOT).endsWith(".java")) {
                try (OutputStream out = zip.addEntry(name, entry)) {
                    IOUtils.copy(zin, out);
                }
            } else if (!sources && !name.toLowerCase(Locale.ROOT).endsWith(".java") && !name.toLowerCase(Locale.ROOT).endsWith(".class")) {
                try (OutputStream out = zip.addEntry(name, entry)) {
                    IOUtils.copy(zin, out);
                }
            }
        }
        zin.close();
    }
}
