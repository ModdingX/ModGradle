package io.github.noeppi_noeppi.tools.modgradle.plugins.mapping;

import net.minecraftforge.gradle.common.util.Artifact;
import org.gradle.api.Project;

import java.nio.file.Path;

public record MappingInfo(
        Project project,
        Artifact mcpConfig,
        Path outputPath
) { }
