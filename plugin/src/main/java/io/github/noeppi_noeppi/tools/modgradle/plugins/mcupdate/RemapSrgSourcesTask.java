package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate;

import io.github.noeppi_noeppi.tools.modgradle.mappings.OfficialNames;
import io.github.noeppi_noeppi.tools.modgradle.mappings.SrgRemapper;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import javax.annotation.WillNotClose;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemapSrgSourcesTask extends DefaultTask {

    public static final Pattern FIELD_PATTERN = Pattern.compile("field_\\d+_\\w+");
    public static final Pattern METHOD_PATTERN = Pattern.compile("func_(\\d+)_\\w+");
    private static final Pattern AT_LINE_FUNC_PATTERN = Pattern.compile("^(\\s*(?:public|private|protected|default)\\s*(?:[+-]f)?\\s*)(\\S+)(\\s+\\w+\\s*)(\\((?:\\[*(?:[ZBCSIJDF]|L.*?;))*\\)\\[*(?:[ZBCSIJDFV]|L.*?;))(.*)$");
    private static final Pattern AT_LINE_PATTERN = Pattern.compile("^(\\s*(?:public|private|protected|default)\\s*(?:[+-]f)?\\s*)(\\S+)(.*)$");
    private static final Pattern COREMOD_SOURCE_PATTERN = Pattern.compile("['\"](?:\\w+\\.)*\\w+['\"]");
    private static final Pattern COREMOD_CLASS_PATTERN = Pattern.compile("['\"](?:\\w+/)*\\w+['\"]");
    private static final Pattern COREMOD_FUNC_PATTERN = Pattern.compile("['\"]\\((?:\\[*(?:[ZBCSIJDF]|L.*?;))*\\)\\[*(?:[ZBCSIJDFV]|L.*?;)['\"]");

    private final Property<FileCollection> files = this.getProject().getObjects().property(FileCollection.class);
    private final Property<URL> source = this.getProject().getObjects().property(URL.class);
    private final Property<URL> target = this.getProject().getObjects().property(URL.class);
    private final Property<URL> official_client = this.getProject().getObjects().property(URL.class);
    private final Property<URL> official_server = this.getProject().getObjects().property(URL.class);

    public RemapSrgSourcesTask() {
        this.files.convention(new DefaultProvider<>(() -> {
            List<Object> files = new ArrayList<>();
            JavaEnv.getJavaSources(this.getProject()).getJava().getSrcDirs().stream()
                    .map(this.getProject()::fileTree)
                    .forEach(files::add);
            JavaEnv.getJavaSources(this.getProject()).getResources().getSrcDirs().stream()
                    .map(f -> f.toPath().resolve("META-INF").resolve("accesstransformer.cfg"))
                    .filter(Files::isRegularFile)
                    .forEach(files::add);
            JavaEnv.getJavaSources(this.getProject()).getResources().getSrcDirs().stream()
                    .map(f -> f.toPath().resolve("coremods"))
                    .filter(Files::isDirectory)
                    .map(this.getProject()::fileTree)
                    .peek(tree -> tree.include("*.js"))
                    .forEach(files::add);
            return this.getProject().files(files.toArray());
        }));
        try {
            // use the latest MCPConfig build we have with old SRG names
            // and the best matching build for the same minecraft version with new SRG names
            this.source.convention(new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.16.5-20210115.111550/mcp_config-1.16.5-20210115.111550.zip"));
            this.target.convention(new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.16.5-20210303.130956/mcp_config-1.16.5-20210303.130956.zip"));
            this.official_client.convention(new URL("https://launcher.mojang.com/v1/objects/374c6b789574afbdc901371207155661e0509e17/client.txt"));
            this.official_server.convention(new URL("https://launcher.mojang.com/v1/objects/41285beda6d251d190f2bf33beadd4fee187df7a/server.txt"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to configure conventions for RemapSrgSourcesTask", e);
        }
        this.getOutputs().upToDateWhen(t -> false);
    }

    @InputFiles
    public FileCollection getFiles() {
        return this.files.get();
    }

    public void setFiles(FileCollection files) {
        this.files.set(files);
    }

    @Input
    public URL getSource() {
        return this.source.get();
    }

    public void setSource(URL source) {
        this.source.set(source);
    }

    @Input
    public URL getTarget() {
        return this.target.get();
    }

    public void setTarget(URL target) {
        this.target.set(target);
    }

    @Input
    public URL getOfficialClient() {
        return this.official_client.get();
    }

    public void setOfficialClient(URL officialClient) {
        this.official_client.set(officialClient);
    }
    
    @Input
    public URL getOfficialServer() {
        return this.official_server.get();
    }

    public void setOfficialServer(URL officialServer) {
        this.official_server.set(officialServer);
    }
    
    @TaskAction
    protected void remapSources(InputChanges inputs) throws IOException {
        Map<String, String> classRemap = OfficialNames.readOfficialClassMap(this.getOfficialClient().openStream(), this.getOfficialServer().openStream());
        SrgRemapper remapper = SrgRemapper.create(this.getSource().openStream(), this.getTarget().openStream(), classRemap, false);
        this.getFiles().forEach(file -> {
            Path path = file.toPath();
            if (Files.exists(path)) {
                try {
                    BufferedReader reader = Files.newBufferedReader(path);
                    StringWriter writer = new StringWriter();
                    remapSource(remapper, reader, writer, path);
                    reader.close();
                    writer.close();
                    BufferedWriter fileWriter = Files.newBufferedWriter(path, StandardOpenOption.TRUNCATE_EXISTING);
                    fileWriter.write(writer.toString());
                    fileWriter.close();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to remap source file: " + path.toAbsolutePath().normalize().toString(), e);
                }
            }
        });
    }
    
    private static void remapSource(SrgRemapper remapper, @WillNotClose BufferedReader reader, @WillNotClose Writer writer, Path path) throws IOException {
        boolean hasLogged = false;
        for (String srcline = reader.readLine(); srcline != null; srcline = reader.readLine()) {
            String line = srcline;
            Matcher fieldMatcher = FIELD_PATTERN.matcher(line);
            line = fieldMatcher.replaceAll(match -> remapper.remapSrg(match.group()).orElse(match.group()));
            Matcher methodMatcher = METHOD_PATTERN.matcher(line);
            line = methodMatcher.replaceAll(match -> remapper.remapSrg(match.group()).orElse(match.group()));
            if (path.getFileName().toString().equalsIgnoreCase("accesstransformer.cfg")) {
                Matcher classMatcher = AT_LINE_FUNC_PATTERN.matcher(line);
                if (classMatcher.matches()) {
                    line = classMatcher.group(1) + remapper.reverseClassName(classMatcher.group(2).replace('.', '/')).map(n -> n.replace('/', '.')).orElse(classMatcher.group(2)) + classMatcher.group(3) + remapper.reverseMethodSignature(classMatcher.group(4)).orElse(classMatcher.group(4)) + classMatcher.group(5);
                } else {
                    classMatcher = AT_LINE_PATTERN.matcher(line);
                    if (classMatcher.matches()) {
                        line = classMatcher.group(1) + remapper.reverseClassName(classMatcher.group(2).replace('.', '/')).map(n -> n.replace('/', '.')).orElse(classMatcher.group(2)) + classMatcher.group(3);
                    }
                }
            } else if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".js")) {
                line = COREMOD_SOURCE_PATTERN.matcher(line).replaceAll(r -> transformLiteral(r.group(), true, remapper::reverseClassName));
                line = COREMOD_CLASS_PATTERN.matcher(line).replaceAll(r -> transformLiteral(r.group(), false, remapper::reverseClassName));
                line = COREMOD_FUNC_PATTERN.matcher(line).replaceAll(r -> transformLiteral(r.group(), false, remapper::reverseMethodSignature));
            }
            writer.write(line + "\n");
            if (!line.equals(srcline) && !hasLogged) {
                hasLogged = true;
                System.out.println("Remapping old to new SRG in " + path.toAbsolutePath().normalize().toString());
            }
        }
    }

    private static String transformLiteral(String literal, boolean sourceForm, Function<String, Optional<String>> action) {
        String toTransform = literal.substring(1, literal.length() - 1);
        if (sourceForm) toTransform = toTransform.replace('.', '/');
        Optional<String> result = action.apply(toTransform);
        if (result.isPresent()) {
            String resultStr = result.get();
            if (sourceForm) resultStr = resultStr.replace('/', '.');
            return literal.charAt(0) + resultStr + literal.charAt(literal.length() - 1);
        } else {
            return literal;
        }
    }
}
