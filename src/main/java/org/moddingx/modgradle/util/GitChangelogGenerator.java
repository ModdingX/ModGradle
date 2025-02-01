package org.moddingx.modgradle.util;

import jakarta.annotation.Nullable;
import org.apache.commons.io.output.NullOutputStream;
import org.gradle.api.Project;
import org.gradle.process.ExecResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GitChangelogGenerator {

    public static String generateChangelog(Project project, @Nullable String commitFormat) {
        StringBuilder errorDiagnostic = new StringBuilder();
        try {
            String logFormat = getLogFormat(commitFormat);
            String commitRange = getCommitRange(project);
            errorDiagnostic.append(" commits=").append(commitRange);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            project.exec(spec -> {
                spec.commandLine("git", "log", logFormat, commitRange);
                spec.setStandardOutput(output);
                spec.setErrorOutput(System.err);
                spec.setIgnoreExitValue(false);
            });
            output.close();
            return output.toString(StandardCharsets.UTF_8).strip();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate git changelog" + (errorDiagnostic.isEmpty() ? "" : " (" + errorDiagnostic.toString().strip() + ")"), e);
        }
    }

    private static String getLogFormat(@Nullable String commitFormat) {
        if (commitFormat == null) {
            return "--pretty=tformat:- %s - %aN";
        } else {
            return "--pretty=tformat:- [%s](" + commitFormat + ") - %aN";
        }
    }

    private static String getCommitRange(Project project) throws IOException {
        // Jenkins
        String jenkinsCommit = System.getenv("GIT_COMMIT");
        String jenkinsPrevCommit = System.getenv("GIT_PREVIOUS_COMMIT");
        String jenkinsPrevSuccessfulCommit = System.getenv("GIT_PREVIOUS_SUCCESSFUL_COMMIT");
        if (jenkinsCommit != null && jenkinsPrevSuccessfulCommit != null) {
            return jenkinsPrevSuccessfulCommit + "..." + jenkinsCommit;
        } else if (jenkinsPrevCommit != null && jenkinsCommit != null) {
            return jenkinsPrevCommit + "..." + jenkinsCommit;
        }

        // Travis
        String travisRange = System.getenv("TRAVIS_COMMIT_RANGE");
        if (travisRange != null) {
            return travisRange;
        }

        // Git tag-based versioning: if a tag points to the HEAD commit, take all commits since the last tag.
        ByteArrayOutputStream currentTagOut = new ByteArrayOutputStream();
        ExecResult currentTagResult = project.exec(spec -> {
            spec.commandLine("git", "describe", "--tags", "--exact-match", "HEAD");
            spec.setStandardOutput(currentTagOut);
            spec.setErrorOutput(NullOutputStream.nullOutputStream());
            spec.setIgnoreExitValue(true);
        });
        currentTagOut.close();
        if (currentTagResult.getExitValue() == 0) {
            String currentTag = currentTagOut.toString(StandardCharsets.UTF_8).strip();
            ByteArrayOutputStream lastTagOut = new ByteArrayOutputStream();
            ExecResult lastTagResult = project.exec(spec -> {
                spec.commandLine("git", "describe", "--tags", "--abbrev=0", "HEAD^");
                spec.setStandardOutput(lastTagOut);
                spec.setErrorOutput(NullOutputStream.nullOutputStream());
                spec.setIgnoreExitValue(true);
            });
            lastTagOut.close();
            if (lastTagResult.getExitValue() == 0) {
                String lastTag = lastTagOut.toString(StandardCharsets.UTF_8).strip();
                return lastTag + "..." + currentTag;
            }
        }

        // We can't find a previous commit, this may be the first run, take everything up to now.
        return jenkinsCommit != null ? jenkinsCommit : "HEAD";
    }
}
