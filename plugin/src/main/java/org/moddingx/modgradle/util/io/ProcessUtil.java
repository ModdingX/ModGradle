package org.moddingx.modgradle.util.io;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public class ProcessUtil {
    
    public static void run(Path dir, String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(buildCommand(command));
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
    
    private static String[] buildCommand(String[] command) {
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            // Windows is a special case as always
            // Give the command to cmd.exe seems to work, although it
            // is really hacky. A better solution would be appreciated.
            String[] newCommand = new String[command.length + 2];
            newCommand[0] = "cmd";
            newCommand[1] = "/c";
            System.arraycopy(command, 0, newCommand, 2, command.length);
            return newCommand;
        } else {
            return command;
        }
    }
}
