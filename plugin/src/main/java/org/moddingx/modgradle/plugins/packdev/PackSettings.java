package org.moddingx.modgradle.plugins.packdev;

import java.util.List;
import java.util.Optional;

public record PackSettings(String minecraft,
                           String forge,
                           int projectId,
                           Optional<String> author,
                           List<String> editions) {
}
