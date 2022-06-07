package org.moddingx.modgradle.plugins.javadoc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraftforge.gradle.mcp.MCPExtension;
import net.minecraftforge.gradle.userdev.UserDevExtension;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.util.IOUtil;
import org.moddingx.modgradle.util.McEnv;
import org.moddingx.modgradle.util.StringUtil;

import javax.inject.Inject;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.List;

public abstract class JavadocLinksTask extends DefaultTask {

    public static final List<String> FILES_TO_COPY = List.of(
            "package-list", "element-list", "module-list"
    );

    private final Javadoc baseTask;
    
    @Inject
    public JavadocLinksTask(Javadoc baseTask) {
        this.baseTask = baseTask;
        Path basePath = this.getProject().file("build").toPath().resolve(this.getName());
        this.getConfig().convention(McEnv.findMinecraftVersion(this.getProject()).map(mcv -> {
            try {
                return new URL("https://assets.moddingx.org/javadoc_links/" + mcv + ".json");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }));
        this.getIncludeMinecraft().convention(true);
        this.getIncludeMcp().convention(this.getProject().provider(() -> {
            boolean hasMcp = false;
            try {
                Object mcpVersion = this.getProject().getExtensions().getExtraProperties().get("MCP_VERSION");
                hasMcp = mcpVersion != null;
            } catch (Exception e) {
                //
            }
            if (!hasMcp) {
                hasMcp = this.getProject().getExtensions().findByType(MCPExtension.class) != null;
            }
            return hasMcp;
        }));
        this.getIncludeForge().convention(this.getProject().provider(() -> this.getProject().getExtensions().findByType(UserDevExtension.class) != null));
        this.getOutput().convention(this.getProject().provider(() -> () -> basePath.resolve("options.txt").toFile()));
        this.getOutputs().dir(basePath.toFile());
    }

    @Input
    public abstract Property<URL> getConfig();

    @Input
    public abstract Property<Boolean> getIncludeMinecraft();

    @Input
    public abstract Property<Boolean> getIncludeMcp();

    @Input
    public abstract Property<Boolean> getIncludeForge();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @TaskAction
    protected void generateJavadocLinks(InputChanges inputs) throws IOException {
        Path libPath = this.getProject().file("build").toPath().resolve(this.getName()).resolve("libs");
        if (!Files.isDirectory(libPath)) {
            Files.createDirectories(libPath);
        }
        if (this.baseTask.getOptions().getDoclet() != null) {
            // Custom doclet, don't add options
            Writer writer = Files.newBufferedWriter(this.getOutput().getAsFile().get().toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            writer.write("\n");
            writer.close();
        } else {
            Reader reader = new BufferedReader(new InputStreamReader(this.getConfig().get().openStream()));
            JsonObject cfg = ModGradle.INTERNAL.fromJson(reader, JsonObject.class);
            reader.close();
            Path output = this.getOutput().getAsFile().get().toPath();
            PathUtils.createParentDirectories(output);
            Writer writer = Files.newBufferedWriter(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            if (cfg.has("minecraft") && this.getIncludeMinecraft().get()) {
                this.addSet(libPath, writer, cfg.getAsJsonArray("minecraft"));
            }
            if (cfg.has("mcp") && this.getIncludeMcp().get()) {
                this.addSet(libPath, writer, cfg.getAsJsonArray("mcp"));
            }
            if (cfg.has("forge") && this.getIncludeForge().get()) {
                this.addSet(libPath, writer, cfg.getAsJsonArray("forge"));
            }
            writer.write("\n");
            writer.close();
        }
    }

    private void addSet(Path libPath, Writer writer, JsonArray elems) throws IOException {
        for (JsonElement entry : elems) {
            JsonObject json = entry.getAsJsonObject();
            URL doc = new URL(json.get("url").getAsString());
            URL res = json.has("res") ? new URL(json.get("res").getAsString()) : null;
            if (res == null) {
                writer.write(" -link " + StringUtil.quote(doc.toString()));
            } else {
                Path targetPath = libPath.resolve(res.toString().replace('/', '_').replace(':', '_'));
                if (Files.exists(targetPath)) {
                    PathUtils.deleteDirectory(targetPath);
                }
                Files.createDirectories(targetPath);
                Path tempFile = Files.createTempFile("javadoc_links", ".jar");
                Files.copy(res.openStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
                try (FileSystem fs = IOUtil.getFileSystem(URI.create("jar:" + tempFile.toUri()))) {
                    for (String fname : FILES_TO_COPY) {
                        Path fpath = fs.getPath(fname);
                        if (Files.exists(fpath)) {
                            Files.copy(fpath, targetPath.resolve(fname), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } finally {
                    Files.deleteIfExists(tempFile);
                }
                writer.write(" -linkoffline " + StringUtil.quote(doc.toString()) + " " + StringUtil.quote(targetPath.toAbsolutePath().normalize().toUri().toString()));
            }
        }
    }
}
