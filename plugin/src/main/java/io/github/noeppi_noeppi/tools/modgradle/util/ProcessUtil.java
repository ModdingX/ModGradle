package io.github.noeppi_noeppi.tools.modgradle.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public class ProcessUtil {
    
    public static void run(Path dir, String... command) throws IOException {
        String[] quoted = new String[command.length];
        for (int i = 0; i < command.length; i++) {
            quoted[i] = quoteWin(command[i]);
        }
        ProcessBuilder pb = new ProcessBuilder(quoted);
        pb.inheritIO();
        pb.directory(dir.toFile());
        try {
            Process proc = pb.start();
            int code = proc.waitFor();
            if (code != 0) {
                throw new IOException("External process returned exit code " + code + ": " + String.join(" ", command));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("External process got interrupted.", e);
        }
    }

    private static String quoteWin(String str) {
        // Windows seems to not like special characters even if you use an argument
        // array to start a process.
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            return '"' + str.replace("\"", "\\\"")
                    .replace("\\", "\\\\") + '"';
        } else {
            return str;
        }
    }
}
