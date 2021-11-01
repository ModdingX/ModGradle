package io.github.noeppi_noeppi.tools.modgradle.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;

public class ProcessUtil {
    
    public static void run(Path dir, String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.directory(dir.toFile());
        try {
            Process proc = pb.start();
            IOUtils.copy(proc.getInputStream(), System.err);
            int code = proc.waitFor();
            if (code != 0) {
                throw new IOException("External process returned exit code " + code + ": " + String.join(" ", command));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("External process got interrupted.", e);
        }
    }
}
