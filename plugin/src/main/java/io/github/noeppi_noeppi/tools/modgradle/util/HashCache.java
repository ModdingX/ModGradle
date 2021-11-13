package io.github.noeppi_noeppi.tools.modgradle.util;

import org.apache.commons.io.file.PathUtils;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class HashCache {
    
    private final Path path;
    private final Map<String, String> hashes;
    private final Map<String, String> staged;

    private HashCache(Path path) throws IOException {
        this.path = path.toAbsolutePath().normalize();
        this.hashes = new HashMap<>();
        this.staged = new HashMap<>();
        if (Files.exists(this.path)) {
            try (BufferedReader reader = Files.newBufferedReader(this.path)) {
                reader.lines().forEach(line -> {
                    if (line.length() > 41) {
                        String hash = line.substring(0, 40);
                        String pathKey = this.pathKey(this.path.resolve(line.substring(41)));
                        this.hashes.put(pathKey, hash);
                    }
                });
            }
        }
    }
    
    public boolean compareAndSet(Path path) throws IOException {
        String key = this.pathKey(path);
        String hash = this.hash(path);
        if (!this.hashes.containsKey(key) || !this.hashes.get(key).equals(hash)) {
            this.hashes.put(key, hash);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean compareAndStage(Path path) throws IOException {
        String key = this.pathKey(path);
        String hash = this.hash(path);
        if (!this.hashes.containsKey(key) || !this.hashes.get(key).equals(hash)) {
            this.staged.put(key, hash);
            return true;
        } else {
            return false;
        }
    }
    
    public void apply() {
        this.hashes.putAll(this.staged);
        this.staged.clear();
    }
    
    public void save() throws IOException {
        if (this.hashes.isEmpty()) {
            Files.deleteIfExists(this.path);
        } else {
            PathUtils.createParentDirectories(this.path);
            BufferedWriter writer = Files.newBufferedWriter(this.path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            for (String pathKey : this.hashes.keySet().stream().sorted().toList()) {
                writer.write(this.hashes.get(pathKey) + " " + pathKey + "\n");
            }
            writer.write("\n");
            writer.close();
        }
    }
    
    private String pathKey(Path p) {
        return this.path.relativize(p.toAbsolutePath().normalize()).normalize().toString();
    }
    
    private String hash(Path path) throws IOException {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            try (InputStream in = new DigestInputStream(Files.newInputStream(path), sha1)) {
                in.readAllBytes();
            }
            return String.format("%040X", new BigInteger(1, sha1.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Hash Algorithm not found.", e);
        }
    }
    
    public static HashCache create(Path path) throws IOException {
        return new HashCache(path);
    }
}
