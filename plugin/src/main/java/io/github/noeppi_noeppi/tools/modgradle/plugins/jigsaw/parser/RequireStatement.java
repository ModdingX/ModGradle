package io.github.noeppi_noeppi.tools.modgradle.plugins.jigsaw.parser;

import java.util.List;

public record RequireStatement(boolean isStatic, boolean isTransitive, List<String> modules) implements Statement { }
