package io.github.noeppi_noeppi.tools.modgradle.util;

import net.minecraftforge.gradle.common.util.Artifact;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.provider.Provider;

import javax.annotation.Nullable;
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
    
    public static Provider<Boolean> isRunningInCI(Project project) {
        return project.provider(() -> {
            String mgCi = System.getenv("MODGRADLE_CI");
            if ("true".equalsIgnoreCase(mgCi)) return true;
            if ("false".equalsIgnoreCase(mgCi)) return false;
            // Auto detection
            return System.getenv("JENKINS_URL") != null;
        });
    }
}
