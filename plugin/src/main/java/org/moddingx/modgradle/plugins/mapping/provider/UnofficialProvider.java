package org.moddingx.modgradle.plugins.mapping.provider;

import org.gradle.api.Project;
import org.moddingx.modgradle.mappings.MappingIO;

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
        MappingIO.NameMappings names = MappingIO.readNames(url.openStream(), true);
        MappingIO.writeNames(out, names.names(), names.docs());
    }
}
