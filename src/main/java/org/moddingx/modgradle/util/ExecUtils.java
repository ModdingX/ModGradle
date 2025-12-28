package org.moddingx.modgradle.util;

import org.gradle.api.Project;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;

public class ExecUtils {

    private ExecUtils() {}

    public static ExecOperations execOps(Project project) {
        return project.getObjects().newInstance(Services.class).getExec();
    }

    public static abstract class Services {
        @Inject
        protected abstract ExecOperations getExec();
    }
}
