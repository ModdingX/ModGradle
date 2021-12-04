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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HashCache {
    
    private final Path path;
    private final Path base;
    private final Map<String, String> hashes;
    private final Map<String, String> staged;

    private HashCache(Path path) throws IOException {
        this.path = path.toAbsolutePath().normalize();
        this.base = this.path.getParent() == null ? this.path : this.path.getParent().toAbsolutePath().normalize();
        this.hashes = new HashMap<>();
        this.staged = new HashMap<>();
        if (Files.exists(this.path)) {
            try (BufferedReader reader = Files.newBufferedReader(this.path)) {
                reader.lines().forEach(line -> {
                    if (line.length() > 41) {
                        String hash = line.substring(0, 40);
                        String pathKey = this.pathKey(this.base.resolve(line.substring(41).replace("/", File.separator)));
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
        this.apply(true);
    }
    
    public void apply(boolean deleteInvalid) {
        this.hashes.putAll(this.staged);
        this.staged.clear();
        if (deleteInvalid) {
            this.hashes.keySet().removeIf(p -> !Files.exists(this.base.resolve(p.replace("/", File.separator))));
        }
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
        Path key = this.base.relativize(p.toAbsolutePath().normalize()).normalize();
        String keyString = IntStream.range(0, key.getNameCount()).mapToObj(key::getName).map(Path::toString).collect(Collectors.joining("/"));
        return keyString.isEmpty() ? "." : keyString;
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
