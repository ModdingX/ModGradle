package io.github.noeppi_noeppi.tools.modgradle.plugins.mapping;

import io.github.noeppi_noeppi.tools.modgradle.mappings.BaseNames;
import io.github.noeppi_noeppi.tools.modgradle.mappings.OldMappingReader;
import io.github.noeppi_noeppi.tools.modgradle.mappings.SrgRemapper;
import io.github.noeppi_noeppi.tools.modgradle.mappings.export.McpExporter;
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier;
import net.minecraftforge.artifactural.base.repository.ArtifactProviderBuilder;
import net.minecraftforge.artifactural.base.repository.SimpleRepository;
import net.minecraftforge.artifactural.gradle.GradleRepositoryAdapter;
import net.minecraftforge.gradle.common.util.BaseRepo;
import net.minecraftforge.gradle.common.util.MavenArtifactDownloader;
import net.minecraftforge.gradle.common.util.POMBuilder;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.mcp.MCPRepo;
import org.gradle.api.Project;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class MappingFakeRepo extends BaseRepo {

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

    private final Project project;
    private final MappingInfo info;

    protected MappingFakeRepo(Project project, MappingInfo info) {
        super(Utils.getCache(project, "modgradle_repo"), project.getLogger());
        this.project = project;
        this.info = info;
    }

    public void load() {
        GradleRepositoryAdapter repo = GradleRepositoryAdapter.add(this.project.getRepositories(), "MODGRADLE_DYNAMIC", this.getCacheRoot(), SimpleRepository.of(
                ArtifactProviderBuilder.begin(ArtifactIdentifier.class).provide(this)
        ));
        // Now we are added to the end of the list but we must be added to the beginning so we are
        // called before MCPRepo.
        // Remove ourselves again
        this.project.getRepositories().remove(repo);
        // And add us to the front
        this.project.getRepositories().add(0, repo);
    }

    @Nullable
    @Override
    protected File findFile(ArtifactIdentifier artifact) throws IOException {
        if ("net.minecraft".equals(artifact.getGroup()) && artifact.getName().startsWith("mappings_")) {
            String channel = artifact.getName().substring(9);
            if (channel.equals("unofficial")) {
                return this.generate("unofficial", "zip", artifact, (version, path) -> {
                    URL url = new URL("https://noeppi-noeppi.github.io/MappingUtilities/mcp_unofficial/" + version + ".zip");
                    BaseNames names = OldMappingReader.readOldMappings(url.openStream(), false, true);
                    BaseNames doc = OldMappingReader.readOldMappings(url.openStream(), true, true);
                    OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    McpExporter.writeMcpZip(out, names, doc);
                });
            } else if (channel.equals("stable2") || channel.equals("snapshot2") ||channel.equals("unofficial2")) {
                // Old names that need to be remapped to new SRG names
                return this.generate(channel, "zip", artifact, (version, path) -> {
                    File base = MavenArtifactDownloader.generate(this.project, MCPRepo.getMappingDep(channel.substring(0, channel.length() - 1), version), false);
                    if (base == null) throw new IllegalStateException("Failed to SRG-remap mappings: base names not found: " + MCPRepo.getMappingDep(channel.substring(0, channel.length() - 1), version));
                    BaseNames names = OldMappingReader.readOldMappings(Files.newInputStream(base.toPath()), false, channel.equals("unofficial2"));
                    BaseNames doc = OldMappingReader.readOldMappings(Files.newInputStream(base.toPath()), true, channel.equals("unofficial2"));
                    SrgRemapper remapper = SrgRemapper.create(SRG_REMAP_SOURCE_CONFIG.openStream(), SRG_REMAP_TARGET_CONFIG.openStream(), null, false);
                    BaseNames remappedNames = remapper.remapNames(names);
                    BaseNames remappedDoc = remapper.remapNames(doc);
                    OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    McpExporter.writeMcpZip(out, remappedNames, remappedDoc);
                });
            }
        }
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private File generate(String channel, String defaultExt, ArtifactIdentifier artifact, MappingGenerator generator) throws IOException {
        if ("pom".equals(artifact.getExtension())) {
            Path dest = this.getOutput(channel, artifact.getVersion(), "pom");
            if (!Files.isRegularFile(dest) || Files.size(dest) <= 0) {
                Files.createDirectories(dest.getParent());
                try {
                    BufferedWriter w = Files.newBufferedWriter(dest);
                    w.write(new POMBuilder("net.minecraft", artifact.getName(), artifact.getVersion()).build() + "\n");
                    w.close();
                } catch (ParserConfigurationException | TransformerException e) {
                    throw new IOException("Failed to generate POM for " + artifact);
                }
            }
            return dest.toFile();
        } else {
            Path dest = this.getOutput(channel, artifact.getVersion(), defaultExt);
            if (!Files.isRegularFile(dest) || Files.size(dest) <= 0) {
                Files.createDirectories(dest.getParent());
                generator.generate(artifact.getVersion(), dest);
            }
            return dest.toFile();
        }
    }

    private Path getOutput(String channel, String version, String ext) {
        return this.info.outputPath().resolve(channel).resolve("v" + version + "." + ext);
    }

    @FunctionalInterface
    private interface MappingGenerator {
        void generate(String version, Path path) throws IOException;
    }
}
