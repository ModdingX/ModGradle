package io.github.noeppi_noeppi.tools.modgradle.plugins.mapping.provider;

import net.minecraftforge.gradle.common.util.MavenArtifactDownloader;
import org.gradle.api.Project;
import org.parchmentmc.librarian.forgegradle.ParchmentChannelProvider;
import org.parchmentmc.librarian.forgegradle.ParchmentMappingVersion;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.Set;

// Waiting for https://github.com/ParchmentMC/Librarian/pull/2
public class SugarcaneProvider extends ParchmentChannelProvider {

    public static final SugarcaneProvider INSTANCE = new SugarcaneProvider();
    
    private SugarcaneProvider() {
        
    }

    @Nonnull
    @Override
    public Set<String> getChannels() {
        return Set.of("sugarcane");
    }
    
    @Nonnull
    @Override
    protected Path getCacheBase(Project project) {
        return MappingsProvider.getBasePath(project, "sugarcane");
    }

    @Nonnull
    @Override
    protected File cacheParchment(Project project, String mcpVersion, String mappingsVersion, String ext) {
        return MappingsProvider.getCacheFile(project, "sugarcane", mappingsVersion, mcpVersion, ext).toFile();
    }

    @Override
    protected File getParchmentZip(Project project, ParchmentMappingVersion version) {
        return MavenArtifactDownloader.download(project, "io.github.noeppi_noeppi.sugarcane:sugarcane-" + version.mcVersion() + ":" + version.parchmentVersion() + "@zip", false);
    }

    @Override
    protected synchronized File getDependency(Project project, String dependencyNotation) {
        // We don't need snapshot handling as SugarCane will only build on releases
        return MavenArtifactDownloader.manual(project, dependencyNotation, false);
    }
}
