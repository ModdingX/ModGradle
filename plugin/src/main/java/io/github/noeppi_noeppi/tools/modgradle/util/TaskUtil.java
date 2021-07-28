package io.github.noeppi_noeppi.tools.modgradle.util;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;

import javax.annotation.Nullable;

public class TaskUtil {
    
    @Nullable
    public static <T extends Task> T getOrNull(Project project, String name, Class<T> cls) {
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
}
