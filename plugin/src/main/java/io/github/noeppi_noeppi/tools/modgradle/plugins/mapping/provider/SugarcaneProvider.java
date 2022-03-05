package io.github.noeppi_noeppi.tools.modgradle.plugins.mapping.provider;

import net.minecraftforge.gradle.common.util.MavenArtifactDownloader;
import net.minecraftforge.gradle.mcp.MCPRepo;
import org.gradle.api.Project;
import org.parchmentmc.feather.mapping.VersionedMappingDataContainer;
import org.parchmentmc.librarian.forgegradle.ParchmentChannelProvider;
import org.parchmentmc.librarian.forgegradle.ParchmentMappingVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SugarcaneProvider extends ParchmentChannelProvider {

    public static final SugarcaneProvider INSTANCE = new SugarcaneProvider();
    
    private SugarcaneProvider() {
        
    }

    @Nonnull
    @Override
    public Set<String> getChannels() {
        return Set.of("sugarcane");
    }

    @Nullable
    @Override
    public File getMappingsFile(MCPRepo mcpRepo, Project project, String channel, String mappingVersion) throws IOException {
        ParchmentMappingVersion version = ParchmentMappingVersion.of(mappingVersion);
        if (!Objects.equals(version.mcpVersion(), version.queryMcVersion())) {
            System.err.println("Using SugarCane for " + version.queryMcVersion() + " on minecraft " + version.mcpVersion() + ". This is discouraged and will most likely result in compilation errors or subtle bugs.");
        }
        return super.getMappingsFile(mcpRepo, project, channel, mappingVersion);
    }

    @Nonnull
    @Override
    protected Path getCacheBase(Project project) {
        return MappingsProvider.getBasePath(project, "sugarcane");
    }

    @Nonnull
    @Override
    protected File cacheParchment(Project project, String queryMcVersion, String mcpVersion, String mappingsVersion, String ext) {
        return MappingsProvider.getCacheFile(project, "sugarcane", (queryMcVersion.isEmpty() ? "null" : queryMcVersion) + "__" + mappingsVersion, mcpVersion, ext).toFile();
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

    @Override
    protected VersionedMappingDataContainer extractMappingData(File dep) throws IOException {
        try (ZipFile zip = new ZipFile(dep)) {
            ZipEntry entry = zip.getEntry("sugarcane.json");
            if (entry == null) throw new IllegalStateException("'sugarcane.json' missing in SugarCane build");

            return GSON.fromJson(new InputStreamReader(zip.getInputStream(entry)), VersionedMappingDataContainer.class);
        }
    }
}
