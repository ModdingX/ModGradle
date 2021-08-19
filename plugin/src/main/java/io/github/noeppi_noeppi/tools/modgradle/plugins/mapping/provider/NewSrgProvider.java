package io.github.noeppi_noeppi.tools.modgradle.plugins.mapping.provider;

import io.github.noeppi_noeppi.tools.modgradle.mappings.BaseNames;
import io.github.noeppi_noeppi.tools.modgradle.mappings.OldMappingReader;
import io.github.noeppi_noeppi.tools.modgradle.mappings.SrgRemapper;
import io.github.noeppi_noeppi.tools.modgradle.mappings.export.OldMcpExporter;
import net.minecraftforge.gradle.common.util.MavenArtifactDownloader;
import net.minecraftforge.gradle.mcp.MCPRepo;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Set;

public class NewSrgProvider extends MappingsProvider {

    public static final NewSrgProvider INSTANCE = new NewSrgProvider();
    
    private static final URL SRG_REMAP_SOURCE_CONFIG;
    private static final URL SRG_REMAP_TARGET_CONFIG;
    static {
        try {
            SRG_REMAP_SOURCE_CONFIG = new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.16.5-20210115.111550/mcp_config-1.16.5-20210115.111550.zip");
            SRG_REMAP_TARGET_CONFIG = new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.16.5-20210303.130956/mcp_config-1.16.5-20210303.130956.zip");
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    private NewSrgProvider() {

    }
    
    @Nonnull
    @Override
    public Set<String> getChannels() {
        return Set.of("stable2", "snapshot2", "unofficial2");
    }

    @Override
    protected void generate(OutputStream out, Project project, String channel, String version) throws IOException {
        File base = MavenArtifactDownloader.generate(project, MCPRepo.getMappingDep(channel.substring(0, channel.length() - 1), version), false);
        if (base == null)
            throw new IllegalStateException("Failed to SRG-remap mappings: base names not found: " + MCPRepo.getMappingDep(channel.substring(0, channel.length() - 1), version));
        BaseNames names = OldMappingReader.readOldMappings(Files.newInputStream(base.toPath()), false, channel.equals("unofficial2"));
        BaseNames doc = OldMappingReader.readOldMappings(Files.newInputStream(base.toPath()), true, channel.equals("unofficial2"));
        SrgRemapper remapper = SrgRemapper.create(SRG_REMAP_SOURCE_CONFIG.openStream(), SRG_REMAP_TARGET_CONFIG.openStream(), null, false);
        BaseNames remappedNames = remapper.remapNames(names);
        BaseNames remappedDoc = remapper.remapNames(doc);
        OldMcpExporter.writeMcpZip(out, remappedNames, remappedDoc);
    }
}
