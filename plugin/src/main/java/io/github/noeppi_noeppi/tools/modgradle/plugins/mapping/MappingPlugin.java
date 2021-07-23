package io.github.noeppi_noeppi.tools.modgradle.plugins.mapping;

import net.minecraftforge.gradle.common.util.Artifact;
import net.minecraftforge.gradle.common.util.MinecraftExtension;
import net.minecraftforge.gradle.mcp.MCPExtension;
import net.minecraftforge.gradle.patcher.PatcherExtension;
import net.minecraftforge.gradle.userdev.UserDevExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// Inject custom mappings and documentation into ForgeGradle
public class MappingPlugin implements Plugin<Project> {

    public static final int MAPPING_VERSION = 1;

    @Override
    public void apply(@Nonnull Project p) {
        p.afterEvaluate(project -> {
            MinecraftExtension mc = findMinecraftExtension(project);

            Artifact mcpArtifact = null;
            try {
                Object mcpVersion = project.getExtensions().getExtraProperties().get("MCP_VERSION");
                if (mcpVersion == null)
                    throw new IllegalStateException("Can't load mapping plugin: MCP_VERSION not set.");
                mcpArtifact = Artifact.from("de.oceanlabs.mcp", "mcp_config", mcpVersion.toString(), null, "zip");
            } catch (ExtraPropertiesExtension.UnknownPropertyException e) {
                // If not using the userdev plugin we look for a MCP extension instead.
                MCPExtension mcpExt = findMCP(project);
                if (mcpExt != null) {
                    mcpArtifact = mcpExt.getConfig().get();
                }
            }
            if (mcpArtifact == null) throw new IllegalStateException("Can't load mapping plugin: MCP_VERSION not set.");

            MappingInfo info = new MappingInfo(
                    project, mcpArtifact,
                    project.file("build/modgradle_mappings").toPath()
            );

            MappingFakeRepo repo = new MappingFakeRepo(project, info);
            repo.load();
        });
    }

    private static MinecraftExtension findMinecraftExtension(Project project) {
        try {
            Object ext = project.getExtensions().getByName(UserDevExtension.EXTENSION_NAME);
            if (ext instanceof MinecraftExtension) return (MinecraftExtension) ext;
        } catch (UnknownDomainObjectException e) {
            //
        }
        try {
            Object ext = project.getExtensions().getByName(PatcherExtension.EXTENSION_NAME);
            if (ext instanceof MinecraftExtension) return (MinecraftExtension) ext;
        } catch (UnknownDomainObjectException e) {
            //
        }
        throw new IllegalStateException("Can't load mapping plugin: No minecraft extension found.");
    }

    @Nullable
    private static MCPExtension findMCP(Project project) {
        MCPExtension ext;
        try {
            ext = project.getExtensions().getByType(MCPExtension.class);
        } catch (UnknownDomainObjectException e) {
            ext = null;
        }
        if (ext == null) {
            for (Project sub : project.getSubprojects()) {
                ext = findMCP(sub);
                if (ext != null) break;
            }
        }
        return ext;
    }
}
