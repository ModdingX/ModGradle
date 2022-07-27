package org.moddingx.modgradle.plugins.packdev;

import java.util.Optional;

public record PackSettings(
        String name,
        String version,
        String minecraft,
        String forge,
        Optional<String> author
) {}
