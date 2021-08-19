package io.github.noeppi_noeppi.tools.modgradle.plugins.mapping.provider;

import io.github.noeppi_noeppi.tools.modgradle.mappings.BaseNames;
import io.github.noeppi_noeppi.tools.modgradle.mappings.OldMappingReader;
import io.github.noeppi_noeppi.tools.modgradle.mappings.export.OldMcpExporter;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Set;

public class UnofficialProvider extends MappingsProvider {

    public static final UnofficialProvider INSTANCE = new UnofficialProvider();
    
    private UnofficialProvider() {
        
    }
    
    @Nonnull
    @Override
    public Set<String> getChannels() {
        return Set.of("unofficial");
    }

    @Override
    protected void generate(OutputStream out, Project project, String channel, String version) throws IOException {
        URL url = new URL("https://noeppi-noeppi.github.io/MappingUtilities/mcp_unofficial/" + version + ".zip");
        BaseNames names = OldMappingReader.readOldMappings(url.openStream(), false, true);
        BaseNames doc = OldMappingReader.readOldMappings(url.openStream(), true, true);
        OldMcpExporter.writeMcpZip(out, names, doc);
    }
}
