package io.github.noeppi_noeppi.tools.modgradle.plugins.mapping.provider;

import io.github.noeppi_noeppi.tools.modgradle.mappings.Names;
import io.github.noeppi_noeppi.tools.modgradle.mappings.export.OldMcpExporter;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class NoneProvider extends MappingsProvider {

    public static final NoneProvider INSTANCE = new NoneProvider();
    
    private NoneProvider() {
        
    }

    @Nonnull
    @Override
    public Set<String> getChannels() {
        return Set.of("none");
    }

    @Override
    protected void generate(OutputStream out, Project project, String channel, String version) throws IOException {
        OldMcpExporter.writeMcpZip(out, Names.EMPTY, Names.EMPTY);
    }
}
