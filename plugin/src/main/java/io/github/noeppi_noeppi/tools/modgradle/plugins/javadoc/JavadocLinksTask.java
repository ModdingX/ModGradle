package io.github.noeppi_noeppi.tools.modgradle.plugins.javadoc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.util.McEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.StringUtil;
import net.minecraftforge.gradle.mcp.MCPExtension;
import net.minecraftforge.gradle.userdev.UserDevExtension;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JavadocLinksTask extends DefaultTask {

    private final Property<URL> config = this.getProject().getObjects().property(URL.class);
    private final Property<Boolean> includeMinecraft = this.getProject().getObjects().property(Boolean.class);
    private final Property<Boolean> includeMcp = this.getProject().getObjects().property(Boolean.class);
    private final Property<Boolean> includeForge = this.getProject().getObjects().property(Boolean.class);
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();

    public JavadocLinksTask() {
        Path basePath = this.getProject().file("build").toPath().resolve(this.getName());
        this.config.convention(new DefaultProvider<>(() -> {
            String mcv = McEnv.findMinecraftVersion(this.getProject());
            return new URL("https://noeppi-noeppi.github.io/MinecraftUtilities/javadoc_links/" + mcv + ".json");
        }));
        this.includeMinecraft.convention(true);
        this.includeMcp.convention(new DefaultProvider<>(() -> {
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
        this.includeForge.convention(new DefaultProvider<>(() -> this.getProject().getExtensions().findByType(UserDevExtension.class) != null));
        this.output.convention(new DefaultProvider<>(() -> (RegularFile) () -> basePath.resolve("options.txt").toFile()));
        this.getOutputs().dir(basePath.toFile());
    }

    @Input
    public URL getConfig() {
        return this.config.get();
    }

    public void setConfig(URL source) {
        this.config.set(source);
    }

    @Input
    public boolean isIncludeMinecraft() {
        return this.includeMinecraft.get();
    }

    public void setIncludeMinecraft(boolean includeMinecraft) {
        this.includeMinecraft.set(includeMinecraft);
    }

    @Input
    public boolean isIncludeMcp() {
        return this.includeMcp.get();
    }

    public void setIncludeMcp(boolean includeMcp) {
        this.includeMcp.set(includeMcp);
    }

    @Input
    public boolean isIncludeForge() {
        return this.includeForge.get();
    }

    public void setIncludeForge(boolean includeForge) {
        this.includeForge.set(includeForge);
    }

    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }

    @TaskAction
    protected void generateJavadocLinks(InputChanges inputs) throws IOException {
        Path libPath = this.getProject().file("build").toPath().resolve(this.getName()).resolve("libs");
        if (!Files.isDirectory(libPath)) {
            Files.createDirectories(libPath);
        }
        Reader reader = new BufferedReader(new InputStreamReader(this.getConfig().openStream()));
        JsonObject cfg = ModGradle.INTERNAL.fromJson(reader, JsonObject.class);
        reader.close();
        Path output = this.getOutput().getAsFile().toPath();
        if (!Files.isDirectory(output.getParent())) {
            Files.createDirectories(output.getParent());
        }
        Writer writer = Files.newBufferedWriter(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        if (cfg.has("minecraft") && this.isIncludeMinecraft()) {
            this.addSet(libPath, writer, cfg.getAsJsonArray("minecraft"));
        }
        if (cfg.has("mcp") && this.isIncludeMcp()) {
            this.addSet(libPath, writer, cfg.getAsJsonArray("mcp"));
        }
        if (cfg.has("forge") && this.isIncludeForge()) {
            this.addSet(libPath, writer, cfg.getAsJsonArray("forge"));
        }
        writer.write("\n");
        writer.close();
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
                ZipInputStream zin = new ZipInputStream(res.openStream());
                for (ZipEntry zf = zin.getNextEntry(); zf != null; zf = zin.getNextEntry()) {
                    String name = zf.getName();
                    while (name.startsWith("/")) name = name.substring(1);
                    Path target = targetPath.resolve(name);
                    if (!zf.isDirectory()) {
                        Files.createDirectories(target.getParent());
                        if (!zf.getName().endsWith(".html") && !zf.getName().endsWith(".htm")
                                && !zf.getName().endsWith(".css") && !zf.getName().endsWith(".js")
                                && !zf.getName().endsWith(".png") && !zf.getName().endsWith(".jpeg")
                                && !zf.getName().endsWith(".jpg") && !zf.getName().endsWith("-search-index.zip")
                                && !zf.getName().endsWith(".gif"))
                        Files.copy(zin, target);
                    }
                }
                writer.write(" -linkoffline " + StringUtil.quote(doc.toString()) + " " + StringUtil.quote(targetPath.toAbsolutePath().normalize().toUri().toString()));
            }
        }
    }
}
