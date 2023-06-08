package org.moddingx.modgradle.util;

import net.minecraftforge.gradle.common.util.Artifact;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.provider.Provider;

import java.util.ArrayList;

public class McEnv {

    // Must be used in afterEvaluate
    public static Provider<String> findMinecraftVersion(Project project) {
        return project.provider(() -> {
            Object mcpVersion = project.getExtensions().getExtraProperties().get("MCP_VERSION");
            if (mcpVersion == null)
                throw new IllegalStateException("Can't resolve minecraft version: Not set");
            String mcpv = mcpVersion.toString();
            if (mcpv.contains("-")) {
                return mcpv.substring(0, mcpv.indexOf('-'));
            } else {
                return mcpv;
            }
        });
    }

    // Must be used in afterEvaluate
    public static Provider<String> findForgeVersion(Project project) {
        return findForge(project).map(artifact -> {
            String ver = artifact.getVersion();
            if (ver.contains("-")) {
                ver = ver.substring(ver.indexOf('-') + 1);
            }
            if (ver.contains("_mapped_")) {
                ver = ver.substring(0, ver.indexOf("_mapped_"));
            }
            if (ver.contains("_at_")) {
                ver = ver.substring(0, ver.indexOf("_at_"));
            }
            return ver;
        });
    }

    // Must be used in afterEvaluate
    public static Provider<Artifact> findForge(Project project) {
        return project.provider(() -> {
            if (!project.getPlugins().hasPlugin("net.minecraftforge.gradle")) {
                throw new IllegalStateException("Can't resolve forge dependency: Forge dependency can only be resolved in userdev environment.");
            }
            Configuration minecraft = project.getConfigurations().getByName("minecraft");
            DependencySet mcDependencies = minecraft.getDependencies();
            Artifact artifact = null;
            for (Dependency dep : new ArrayList<>(mcDependencies)) { // Copied to new list to avoid ConcurrentModificationException
                if (dep.getGroup() == null || dep.getVersion() == null) {
                    throw new IllegalStateException("Can't resolve forge dependency: Group or version is null: " + dep.getGroup() + ":" + dep.getName() + ":" + dep.getVersion());
                } else {
                    Artifact currentArtifact = Artifact.from(dep.getGroup(), dep.getName(), dep.getVersion(), null, null);
                    if (artifact == null) {
                        artifact = currentArtifact;
                    } else if (!MgUtil.sameArtifact(artifact, currentArtifact)) {
                        // Sometimes the forge configuration contains the same dependency multiple times.
                        // In that case we just ignore it.
                        throw new IllegalStateException("Can't resolve forge dependency: Multiple entries in " + minecraft.getName() + " configuration.");
                    }
                }
            }
            if (artifact == null) {
                throw new IllegalStateException("Can't resolve forge dependency: " + minecraft.getName() + " configuration is empty.");
            } else {
                return artifact;
            }
        });
    }
}
