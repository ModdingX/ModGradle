package org.moddingx.modgradle.util.io;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import net.minecraftforge.gradle.common.util.Artifact;
import org.apache.commons.io.function.IOSupplier;
import org.gradle.api.Project;
import org.moddingx.modgradle.api.MixinVersion;
import org.moddingx.modgradle.api.Versioning;
import org.moddingx.modgradle.util.McEnv;

import javax.annotation.Nullable;
import javax.annotation.WillNotClose;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CopyHelper {
    
    private static final Pattern MIXIN_MIN_VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+)\\.\\d+");
    
    public static void copy(Project project, Path from, Path to, OpenOption... open) throws IOException {
        copy(project, from, to, Map.of(), open);
    }

    public static void copy(Project project, Path from, Path to, Map<String, ?> replacements, OpenOption... open) throws IOException {
        try (Reader reader = Files.newBufferedReader(from, StandardCharsets.UTF_8)) {
            copy(project, reader, to, replacements, open);
        }
    }
    
    public static void copy(Project project, @WillNotClose InputStream in, Path to, OpenOption... open) throws IOException {
        copy(project, in, to, Map.of(), open);
    }
    
    public static void copy(Project project, @WillNotClose InputStream in, Path to, Map<String, ?> replacements, OpenOption... open) throws IOException {
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        copy(project, reader, to, replacements, open);
    }
    
    public static void copy(Project project, @WillNotClose Reader reader, Path to, OpenOption... open) throws IOException {
        copy(project, reader, to, Map.of(), open);
    }
    
    public static void copy(Project project, @WillNotClose Reader reader, Path to, Map<String, ?> replacements, OpenOption... open) throws IOException {
        expand(project, reader, () -> Files.newBufferedWriter(to, StandardCharsets.UTF_8, open), replacements, true, true);
    }
    
    public static void copyTemplateDir(Project project, Path from, Path to, CopyOption... options) throws IOException {
        copyTemplateDir(project, from, to, Map.of(), options);
    }
    
    public static void copyTemplateDir(Project project, Path from, Path to, Map<String, ?> replacements, CopyOption... options) throws IOException {
        copyTemplateDir(project, from, to, null, Map.of(), options);
    }
    
    public static void copyTemplateDir(Project project, Path from, Path to, @Nullable Path renameFile, Map<String, ?> replacements, CopyOption... options) throws IOException {
        if (!Files.isDirectory(to)) {
            throw new IOException("Target directory does not exist: " + to.toAbsolutePath());
        }
        
        FullReplacements map = makeReplacements(project, replacements);
        
        record Rename(String from, String to) {}
        List<Rename> renames;
        if (renameFile == null) {
            renames = List.of();
        } else {
            try (Stream<String> lines = Files.lines(renameFile, StandardCharsets.UTF_8)) {
                renames = lines
                        .map(String::strip)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))
                        .map(line -> line.split(" +"))
                        .map(parts -> {
                            if (parts.length != 2) throw new UncheckedIOException(new IOException("Invalid path replacement: [ " + String.join(" ; ", parts) + " ]"));
                            while (parts[0].startsWith("/")) parts[0] = parts[0].substring(1);
                            while (parts[1].startsWith("/")) parts[1] = parts[1].substring(1);
                            try {
                                return new Rename(expandStringWithFullMap(parts[0], map), expandStringWithFullMap(parts[1], map));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        })
                        .toList();
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
        
        boolean skipExisting = Arrays.asList(options).contains(TemplateCopyOption.SKIP_EXISTING);
        boolean replaceExisting = Arrays.asList(options).contains(StandardCopyOption.REPLACE_EXISTING);
        CopyOption[] copyOptionsToPassAround = Arrays.stream(options).filter(option -> option != TemplateCopyOption.SKIP_EXISTING).toArray(CopyOption[]::new);
        Files.walkFileTree(from, new FileVisitor<>() {
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path target = this.target(dir);
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (fileName.endsWith(".template")) {
                    // Must call target without the template extension, so renames work properly
                    Path target = this.target(file.toAbsolutePath().getParent().resolve(fileName.substring(0, fileName.length() - 9)));
                    if (Files.exists(target)) {
                        if (skipExisting) return FileVisitResult.CONTINUE;
                        if (!replaceExisting) throw new FileAlreadyExistsException(target.toAbsolutePath().toString());
                    }
                    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        expandFull(reader, () -> {
                            if (target.getParent() != null) Files.createDirectories(target.getParent());
                            return Files.newBufferedWriter(target, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        }, map, true, true);
                    }
                } else {
                    Path target = this.target(file);
                    if (!skipExisting || !Files.exists(target)) {
                        if (target.getParent() != null) Files.createDirectories(target.getParent());
                        Files.copy(file, this.target(file), copyOptionsToPassAround);
                    }
                }
                
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                throw e;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
            
            private Path target(Path path) throws IOException {
                Path rel = from.relativize(path);
                FileSystem relFs = rel.getFileSystem();
                for (Rename rename : renames) {
                    Path relRenamePath = relFs.getPath(rename.from().replace("/", relFs.getSeparator()));
                    Path renamed = relFs.getPath(rename.to().replace("/", relFs.getSeparator()));
                    if (!relRenamePath.isAbsolute() && !renamed.isAbsolute() && rel.startsWith(relRenamePath)) {
                        Path partAfterRename = relRenamePath.relativize(rel);
                        rel = renamed.resolve(partAfterRename);
                        break;
                    }
                }
                Path target;
                if (Objects.equals(to.getFileSystem(), rel.getFileSystem())) {
                    target = to.resolve(rel);
                } else {
                    target = to.resolve(rel.toString().replace(rel.getFileSystem().getSeparator(), to.getFileSystem().getSeparator()));
                }
                if (!target.toAbsolutePath().startsWith(to.toAbsolutePath())) {
                    throw new IOException("Directory escape: Tried to access " + target.toAbsolutePath() + " from " + to.toAbsolutePath());
                }
                return target;
            }
        });
    }
    
    public static void expand(Project project, @WillNotClose InputStream in, @WillNotClose OutputStream out) throws IOException {
        expand(project, in, out, Map.of());
    }
    
    public static void expand(Project project, @WillNotClose InputStream in, @WillNotClose OutputStream out, Map<String, ?> replacements) throws IOException {
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        expand(project, reader, writer, replacements);
    }
    
    private static void expand(Project project, @WillNotClose Reader reader, @WillNotClose Writer writer) throws IOException {
        expand(project, reader, writer, Map.of());
    }
    
    private static void expand(Project project, @WillNotClose Reader reader, @WillNotClose Writer writer, Map<String, ?> replacements) throws IOException {
        expand(project, reader, () -> writer, replacements, false, false);
    }
    
    private static void expand(Project project, @WillNotClose Reader reader, IOSupplier<Writer> writerFactory, Map<String, ?> replacements, boolean canAbort, boolean closeOutput) throws IOException {
        FullReplacements map = makeReplacements(project, replacements);
        expandFull(reader, writerFactory, map, canAbort, closeOutput);
    }
    
    private static FullReplacements makeReplacements(Project project, Map<String, ?> replacements) {
        Map<String, Object> map = new HashMap<>(replacements);
        if (project.getState().getExecuted()) {
            map.putIfAbsent("minecraft", McEnv.findMinecraftVersion(project).get());
            map.putIfAbsent("forge", McEnv.findForgeVersion(project).get());
            Artifact artifact = McEnv.findForge(project).get();
            map.putIfAbsent("userdev", artifact.getName());
        }
        if (map.get("minecraft") instanceof String minecraft) {
            map.putIfAbsent("jdk", Versioning.getJavaVersion(minecraft));
            int resource = Versioning.getResourceVersion(minecraft);
            int data = Versioning.getDataVersion(minecraft).orElse(resource);
            map.putIfAbsent("resource", resource);
            map.putIfAbsent("data", data);
            map.putIfAbsent("pack", Math.max(resource, data));

            MixinVersion mixin = Versioning.getMixinVersion(minecraft).orElse(null);
            if (mixin != null) {
                map.putIfAbsent("mixin", mixin.release());
                Matcher m = MIXIN_MIN_VERSION_PATTERN.matcher(mixin.release());
                if (m.matches()) {
                    map.putIfAbsent("mixin_min", m.group(1));
                } else {
                    map.putIfAbsent("mixin_min", mixin.release());
                }
                map.putIfAbsent("mixin_compatibility", mixin.compatibility());
            } else {
                map.putIfAbsent("mixin", null);
                map.putIfAbsent("mixin_min", null);
                map.putIfAbsent("mixin_compatibility", null);
            }
        }
        if (map.get("forge") instanceof String forge) {
            map.putIfAbsent("fml", forge.contains(".") ? forge.substring(0, forge.indexOf('.')) : forge);
        }

        map.putIfAbsent("group", project.getGroup().toString());
        map.putIfAbsent("name", project.getName());
        map.putIfAbsent("version", project.getVersion().toString());

        map.put("project", project);
        map.put("gradle", project.getGradle());
        return new FullReplacements(Collections.unmodifiableMap(map));
    }

    private static String expandStringWithFullMap(String string, FullReplacements replacements) throws IOException {
        StringWriter writer = new StringWriter();
        try (StringReader reader = new StringReader(string)) {
            expandFull(reader, () -> writer, replacements, false, true);
        }
        return writer.toString();
    }
    
    private static void expandFull(@WillNotClose Reader reader, IOSupplier<Writer> writerFactory, FullReplacements replacements, boolean canAbort, boolean closeOutput) throws IOException {
        Map<String, Object> fullMap = new HashMap<>(replacements.map());
        fullMap.put("abort", new AbortAction(canAbort));
        
        StringWriter buf;
        Writer writer;
        if (canAbort) {
            buf = new StringWriter();
            writer = buf;
        } else {
            buf = null;
            writer = writerFactory.get();
        }
        
        try {
            SimpleTemplateEngine engine = new SimpleTemplateEngine();
            engine.setEscapeBackslash(false);
            Template template = engine.createTemplate(reader);
            Writable result = template.make(fullMap);
            result.writeTo(writer);
            writer.flush();
            if (closeOutput) writer.close();
        } catch (CopyAbortException e) {
            if (canAbort) {
                return;
            } else {
                throw e;
            }
        } finally {
            if (closeOutput) {
                try {
                    writer.close();
                } catch (Exception e) {
                    //
                }
            }
        }
        
        if (canAbort) {
            writer.close();
            Writer downstream = writerFactory.get();
            downstream.write(buf.toString());
            downstream.flush();
            if (closeOutput) writer.close();
        }
    }
    
    private record FullReplacements(Map<String, Object> map) {}
    
    private static class AbortAction extends GroovyObjectSupport {
        
        private final boolean canAbort;

        private AbortAction(boolean canAbort) {
            this.canAbort = canAbort;
        }

        public void call() {
            if (this.canAbort) {
                throw new CopyAbortException();
            } else {
                throw new IllegalStateException("Can't abort copying this file.");
            }
        }
    }
    
    private static class CopyAbortException extends RuntimeException {}
}
