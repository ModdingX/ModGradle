package io.github.noeppi_noeppi.tools.modgradle.plugins.jigsaw.parser;

import io.github.noeppi_noeppi.tools.modgradle.util.PackageMatcher;

import javax.annotation.Nullable;
import java.util.List;

public record OpensStatement(PackageMatcher packages, @Nullable List<String> targets) implements Statement { }
