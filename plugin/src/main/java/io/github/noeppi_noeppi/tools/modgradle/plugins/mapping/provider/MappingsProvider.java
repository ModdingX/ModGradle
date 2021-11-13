package io.github.noeppi_noeppi.tools.modgradle.plugins.mapping.provider;

import net.minecraftforge.gradle.mcp.ChannelProvider;
import net.minecraftforge.gradle.mcp.MCPRepo;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public abstract class MappingsProvider implements ChannelProvider {

    // Increment this to drop all previously cached versions
    // Might be required if bigger changes are done to the providers
    public static final int SYSTEM_VERSION = 3;

    @Nullable
    @Override
    public File getMappingsFile(@Nonnull MCPRepo mcpRepo, @Nonnull Project project, @Nonnull String channel, @Nonnull String version) throws IOException {
        String hash = Long.toHexString(this.hash(project, channel, version));
        Path path = getCacheFile(project, channel, version, hash, "zip");
        if (!Files.exists(path) || Files.size(path) <= 0) {
            PathUtils.createParentDirectories(path);
            OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            this.generate(out, project, channel, version);
            out.close();
        }
        return path.toFile();
    }
    
    protected long hash(Project project, String channel, String version) {
        return 0;
    }
    
    protected abstract void generate(OutputStream out, Project project, String channel, String version) throws IOException;
    
    public static Path getBasePath(Project project, String channel) {
        return project.file("build").toPath().resolve("modgradle_mappings").resolve("v" + SYSTEM_VERSION)
                .resolve(channel.replace('/', '_'));
    }
    
    public static Path getCacheFile(Project project, String channel, String version, String hash, String ext) {
        return getBasePath(project, channel).resolve(version.replace('/', '_'))
                .resolve(hash).resolve("mappings-" + channel.replace('/', '_') + "-" + version.replace('/', '_') + "-" + hash.replace('/', '_') + "." + ext);
    }
}
