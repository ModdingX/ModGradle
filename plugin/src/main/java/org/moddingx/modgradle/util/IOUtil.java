package org.moddingx.modgradle.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.file.*;
import java.util.Map;

public class IOUtil {

    public static void copyFile(Path from, Path to, Map<String, String> replace, boolean replaceFile) throws IOException {
        if (Files.isRegularFile(from) && (replaceFile || !Files.exists(to))) {
            String content = Files.readString(from);
            writeReplaced(content, to, replace);
        }
    }

    public static void copyFile(InputStream from, Path to, Map<String, String> replace, boolean replaceFile) throws IOException {
        if (replaceFile || !Files.exists(to)) {
            Reader reader = new InputStreamReader(from);
            String content = IOUtils.toString(reader);
            reader.close();
            writeReplaced(content, to, replace);
        }
    }

    private static void writeReplaced(String content, Path to, Map<String, String> replace) throws IOException {
        for (String replaceKey : replace.keySet().stream().sorted().toList()) {
            content = content.replace("${" + replaceKey + "}", replace.get(replaceKey));
        }
        content = content.replace("$$", "$");
        Files.writeString(to, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    public static FileSystem getFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.newFileSystem(uri, Map.of());
        } catch (FileSystemAlreadyExistsException e) {
            return FileSystems.getFileSystem(uri);
        }
    }
}
