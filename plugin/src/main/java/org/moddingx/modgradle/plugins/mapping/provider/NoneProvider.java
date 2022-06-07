package org.moddingx.modgradle.plugins.mapping.provider;

import org.gradle.api.Project;
import org.moddingx.modgradle.mappings.Javadocs;
import org.moddingx.modgradle.mappings.MappingIO;
import org.moddingx.modgradle.mappings.Names;

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
        MappingIO.writeNames(out, Names.EMPTY, Javadocs.EMPTY);
    }
}
