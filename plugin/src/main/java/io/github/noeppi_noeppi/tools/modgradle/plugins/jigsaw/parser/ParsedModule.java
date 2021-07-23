package io.github.noeppi_noeppi.tools.modgradle.plugins.jigsaw.parser;

import java.util.List;

public record ParsedModule(String name, boolean open, boolean exported, List<Statement> statements) {

}
