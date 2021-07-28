package io.github.noeppi_noeppi.tools.modgradle.plugins.sourcejar;

import io.github.noeppi_noeppi.tools.modgradle.util.StringUtil;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

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

public class MergeJarWithSourcesTask extends DefaultTask {
    
    private final Property<RegularFile> base = this.getProject().getObjects().fileProperty();
    private final Property<RegularFile> sources = this.getProject().getObjects().fileProperty();
    private final Property<RegularFile> output = this.getProject().getObjects().fileProperty();

    @InputFile
    public RegularFile getBase() {
        return this.base.get();
    }

    public void setBase(RegularFile base) {
        this.base.set(base);
    }

    @InputFile
    public RegularFile getSources() {
        return this.sources.get();
    }

    public void setSources(RegularFile sources) {
        this.sources.set(sources);
    }

    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }

    @TaskAction
    protected void mergeJars(InputChanges inputs) throws IOException {
        ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(this.getOutput().getAsFile().toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
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
