package io.github.noeppi_noeppi.tools.modgradle.util;

import net.minecraftforge.gradle.common.util.Artifact;

import java.util.Objects;

public class ArtifactUtil {

    public static boolean sameArtifact(Artifact first, Artifact second) {
        return first.getGroup().equals(second.getGroup())
                && first.getName().equals(second.getName())
                && first.getVersion().equals(second.getVersion())
                && Objects.equals(first.getClassifier(), second.getClassifier())
                && Objects.equals(first.getExtension(), second.getExtension());
    }
}
