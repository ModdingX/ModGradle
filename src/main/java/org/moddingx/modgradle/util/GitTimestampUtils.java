package org.moddingx.modgradle.util;

import jakarta.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.process.ExecResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GitTimestampUtils {

    @Nullable
    public static String tryGetCommitTimestamp(Project project) {
        try {
            String commitEnv = System.getenv("GIT_COMMIT");
            String ref = (commitEnv == null || commitEnv.isEmpty()) ? "HEAD" : commitEnv;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ExecResult result = project.exec(spec -> {
                spec.commandLine("git", "show", "-s", "--format=%cd", "--date=iso-strict", ref);
                spec.setStandardOutput(output);
                spec.setErrorOutput(System.err);
                spec.setIgnoreExitValue(true);
            });
            output.close();
            if (result.getExitValue() == 0) {
                String date = output.toString(StandardCharsets.UTF_8).strip();
                return date.isEmpty() ? null : date;
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not determine current git commit time.", e);
        }
    }
}
