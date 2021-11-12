package io.github.noeppi_noeppi.tools.modgradle.api;

import io.github.noeppi_noeppi.tools.modgradle.mappings.MappingIO;
import io.github.noeppi_noeppi.tools.modgradle.mappings.OfficialNames;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;
import net.minecraftforge.srgutils.IRenamer;

import java.io.IOException;
import java.net.URL;

/**
 * Provides some utility methods to read mapping data
 */
public class MappingData {

    /**
     * Loads mappings from SRG names to official names for a given MCPConfig version.
     */
    public static IMappingFile loadSrgOfficial(String mcpConfig) throws IOException {
        String mcv = mcpConfig.contains("-") ? mcpConfig.substring(0, mcpConfig.indexOf('-')) : mcpConfig;
        McpConfigInfo info = McpConfigInfo.getInfo(mcpConfig);
        INamedMappingFile nmf = MappingIO.readMcpConfigSrg(new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/" + mcpConfig + "/mcp_config-" + mcpConfig + ".zip").openStream());
        IMappingFile srg = nmf.getMap("obf", "srg");
        IMappingFile official = OfficialNames.readOfficialMappings(mcv);
        // Is now official -> srg
        IMappingFile target = official.reverse().chain(srg);
        if (info.official) {
            target = target.rename(new IRenamer() {
                
                @Override
                public String rename(IMappingFile.IClass value) {
                    // Rename all SRG classes to their official names
                    return value.getOriginal();
                }
            });
        }
        // Make it srg -> official
        return target.reverse();
    }
    
    /**
     * Loads mappings from SRG names to SRG ids for a given MCPConfig version.
     */
    public static IMappingFile loadSrgId(String mcpConfig) throws IOException {
        String mcv = mcpConfig.contains("-") ? mcpConfig.substring(0, mcpConfig.indexOf('-')) : mcpConfig;
        McpConfigInfo info = McpConfigInfo.getInfo(mcpConfig);
        INamedMappingFile nmf = MappingIO.readMcpConfigSrg(new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/" + mcpConfig + "/mcp_config-" + mcpConfig + ".zip").openStream());
        IMappingFile srgId = nmf.getMap("srg", "id");
        if (info.official) {
            IMappingFile srgObf = nmf.getMap("srg", "obf");
            IMappingFile official = OfficialNames.readOfficialMappings(mcv);
            IMappingFile classLookup = srgObf.chain(official);
            srgId = srgId.reverse().rename(new IRenamer() {
                
                @Override
                public String rename(IMappingFile.IClass value) {
                    return classLookup.remapClass(value.getMapped());
                }
            }).reverse();
        }
        return srgId;
    }
}
