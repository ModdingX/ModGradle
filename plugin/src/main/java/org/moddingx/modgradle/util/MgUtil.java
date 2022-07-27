package org.moddingx.modgradle.util;

import net.minecraftforge.gradle.common.util.Artifact;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.provider.Provider;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

public class MgUtil {

    public static boolean sameArtifact(Artifact first, Artifact second) {
        return first.getGroup().equals(second.getGroup())
                && first.getName().equals(second.getName())
                && first.getVersion().equals(second.getVersion())
                && Objects.equals(first.getClassifier(), second.getClassifier())
                && Objects.equals(first.getExtension(), second.getExtension());
    }

    @Nullable
    public static <T extends Task> T task(Project project, String name, Class<T> cls) {
        try {
            Task task = project.getTasks().getByName(name);
            if (cls.isAssignableFrom(task.getClass())) {
                //noinspection unchecked
                return (T) task;
            } else {
                return null;
            }
        } catch (UnknownTaskException | ClassCastException e) {
            return null;
        }
    }

    public static String dependencyName(Dependency dependency) {
        if (dependency instanceof ExternalModuleDependency emd) {
            return emd.getGroup() + ":" + emd.getName() + ":" + emd.getVersion();
        } else {
            return dependency.toString();
        }
    }
    
    public static Provider<Boolean> isRunningInCI(Project project) {
        return project.provider(() -> {
            String mgCi = System.getenv("MODGRADLE_CI");
            if ("true".equalsIgnoreCase(mgCi)) return true;
            if ("false".equalsIgnoreCase(mgCi)) return false;
            // Auto detection
            return System.getenv("JENKINS_URL") != null;
        });
    }
    
    public static String capitalize(String str) {
        if (str.isEmpty()) {
            return "";
        } else {
            return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
        }
    }
}
