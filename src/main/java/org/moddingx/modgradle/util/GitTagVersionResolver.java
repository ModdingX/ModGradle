package org.moddingx.modgradle.util;

import jakarta.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.process.ExecResult;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GitTagVersionResolver {

    private static final List<String> GIT_REF_NAME_ENVIRONMENT = List.of(
            "GITHUB_REF", "GITHUB_REF_NAME", "GIT_COMMIT_TAG", "CI_COMMIT_REF_NAME"
    );

    public static String getVersion(Project project, @Nullable String defaultVersion) throws IOException {
        Optional<String> describeTag = getVersionFromDescribe(project);
        if (describeTag.isPresent()) return describeTag.get();
        Set<String> tags = listAllTags(project);
        for (String env : GIT_REF_NAME_ENVIRONMENT) {
            String value = System.getenv(env);
            if (value == null) continue;
            if (value.startsWith("refs/tags/")) value = value.substring(10);
            if (tags.contains(value)) return value;
        }
        if (defaultVersion != null) {
            System.err.println("The current git commit is not associated with any tags, using fallback version: " + defaultVersion);
            return defaultVersion;
        } else {
            throw new IllegalStateException("The current commit is not a git tag, and no fallback version is set.");
        }
    }

    private static Optional<String> getVersionFromDescribe(Project project) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExecResult result = ExecUtils.execOps(project).exec(spec -> {
            spec.commandLine("git", "describe", "--tags", "--exact-match", "HEAD");
            spec.setStandardOutput(output);
            spec.setErrorOutput(System.err);
            spec.setIgnoreExitValue(true);
        });
        output.close();
        if (result.getExitValue() != 0) return Optional.empty(); // No tag
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(output.toByteArray())))) {
            return reader.lines().findAny().map(String::strip);
        }
    }

    private static Set<String> listAllTags(Project project) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExecResult result = ExecUtils.execOps(project).exec(spec -> {
            spec.commandLine("git", "tag", "--list");
            spec.setStandardOutput(output);
            spec.setErrorOutput(System.err);
            spec.setIgnoreExitValue(true);
        });
        output.close();
        if (result.getExitValue() != 0) {
            throw new IllegalStateException("git tag --list failed.");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(output.toByteArray())))) {
            return reader.lines().map(String::strip).collect(Collectors.toUnmodifiableSet());
        }
    }
}
