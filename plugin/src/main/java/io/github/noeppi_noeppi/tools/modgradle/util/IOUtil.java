package io.github.noeppi_noeppi.tools.modgradle.util;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;

import javax.annotation.WillClose;
import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    
    public static Map<String, String> commonHashes(@WillClose InputStream in) throws IOException {
        MessageDigest sha1;
        MessageDigest sha256;
        MessageDigest md5;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
            sha256 = MessageDigest.getInstance("SHA256");
            md5 = MessageDigest.getInstance("MD5");
            try (
                    InputStream in1 = new DigestInputStream(in, sha1);
                    InputStream in2 = new DigestInputStream(in1, sha256);
                    InputStream in3 = new DigestInputStream(in2, md5)
            ) {
                in3.readAllBytes();
            }
            ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
            map.put("sha1", String.format("%040X", new BigInteger(1, sha1.digest())));
            map.put("sha256", String.format("%064X", new BigInteger(1, sha256.digest())));
            map.put("md5", String.format("%032X", new BigInteger(1, md5.digest())));
            return map.build();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Failed to compute file hashes", e);
        }
    }
    
    public static String quote(String str) {
        return "\"" + str.replace("\"", "\\\"").replace("\\", "\\\\") + "\"";
    }
}
