package io.github.noeppi_noeppi.tools.modgradle.plugins.packdev;

import java.util.Optional;

public record PackSettings(
        String minecraft,
        String forge,
        int projectId,
        Optional<String> author
) {}
