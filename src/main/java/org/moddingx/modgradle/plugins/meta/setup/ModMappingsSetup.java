package org.moddingx.modgradle.plugins.meta.setup;

import jakarta.annotation.Nullable;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.gradle.api.Project;
import org.moddingx.modgradle.plugins.meta.delegate.ModMappingsConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.Objects;

public class ModMappingsSetup {

    public static void configureBuild(ModContext mod, ModMappingsConfig config) {
        Void unused = switch (config.strategy) {
            case ModMappingsConfig.Strategy.Official.INSTANCE -> null;
            case ModMappingsConfig.Strategy.Parchment parchmentConfig -> {
                Subsystems subsystems = mod.project().getExtensions().getByType(Subsystems.class);
                subsystems.parchment(parchment -> {
                    if (parchmentConfig.artifact() != null) {
                        parchment.enabled(true);
                        parchment.addRepository(true);
                        parchment.parchmentArtifact(parchmentConfig.artifact().getDescriptor());
                    } else {
                        String minecraftVersion = Objects.requireNonNullElse(parchmentConfig.minecraft(), mod.minecraft());
                        String parchmentVersion = parchmentConfig.version();
                        if (parchmentVersion == null) parchmentVersion = latestParchmentVersion(mod.project(), minecraftVersion, parchmentConfig.minecraft() == null);
                        if (parchmentVersion != null) {
                            parchment.enabled(true);
                            parchment.addRepository(true);
                            parchment.minecraftVersion(minecraftVersion);
                            parchment.mappingsVersion(parchmentVersion);
                        }
                    }
                });
                yield null;
            }
        };
    }

    @Nullable
    private static String latestParchmentVersion(Project project, String minecraft, boolean silentFail) {
        if (project.getGradle().getStartParameter().isOffline()) return null;
        try {
            URL metadataUrl = new URI("https://maven.parchmentmc.org/org/parchmentmc/data/parchment-" + minecraft + "/maven-metadata.xml").toURL();
            Metadata metadata;
            try (InputStream in = metadataUrl.openStream()) {
                metadata = new MetadataXpp3Reader().read(in);
            } catch (FileNotFoundException | NoSuchFileException e) {
                if (silentFail) return null;
                throw new RuntimeException("There is no parchment for " + minecraft);
            }
            if (metadata.getVersioning() == null || metadata.getVersioning().getRelease() == null) {
                if (silentFail) return null;
                throw new RuntimeException("There is no parchment for " + minecraft);
            }
            return metadata.getVersioning().getRelease();
        } catch (IOException | URISyntaxException | XmlPullParserException e) {
            if (silentFail) return null;
            throw new RuntimeException("Could not determine latest parchment version for " + minecraft);
        }
    }
}
