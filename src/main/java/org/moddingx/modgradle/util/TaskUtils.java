package org.moddingx.modgradle.util;

import jakarta.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

public class TaskUtils {
    
    @Nullable
    public static <T extends Task> T find(Project project, String name, Class<T> cls) {
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
    
    public static Provider<RegularFile> defaultOutputFile(Task task, String fileName) {
        return task.getProject().getLayout().file(task.getProject().provider(() -> task.getProject().file("build").toPath().resolve(task.getName()).resolve(fileName).toFile()));
    }
    
    public static Provider<Directory> defaultOutputDirectory(Task task, String dirName) {
        return task.getProject().getLayout().dir(task.getProject().provider(() -> task.getProject().file("build").toPath().resolve(task.getName()).resolve(dirName).toFile()));
    }
}
