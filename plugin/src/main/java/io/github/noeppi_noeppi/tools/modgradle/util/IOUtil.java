package io.github.noeppi_noeppi.tools.modgradle.util;

import com.google.common.collect.ImmutableMap;

import javax.annotation.WillClose;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class IOUtil {
    
    public static String readUntil(Reader reader, char chr) throws IOException {
        String str = readTo(reader, chr);
        return str.isEmpty() ? "" : str.substring(0, str.length() - 1);
    }
    
    public static String readTo(Reader reader, char chr) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int read = reader.read();
            if (read == -1) return sb.toString();
            sb.append((char) read);
            if ((char) read == chr) return sb.toString();
        }
    }
    
    public static void skipWhitespace(PushbackReader reader) throws IOException {
        while (true) {
            int read = reader.read();
            if (read == -1) return;
            if (!Character.isWhitespace((char) read)) {
                reader.unread(read);
                return;
            }
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
            map.put("sha1", new BigInteger(1, sha1.digest()).toString(16));
            map.put("sha256", new BigInteger(1, sha256.digest()).toString(16));
            map.put("md5", new BigInteger(1, md5.digest()).toString(16));
            return map.build();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Failed to compute file hashes", e);
        }
    }
}
