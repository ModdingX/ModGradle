package org.moddingx.modgradle.plugins.javadoc;

import jakarta.annotation.Nullable;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.work.InputChanges;
import org.moddingx.modgradle.util.TaskUtils;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class JavadocLinksTask extends DefaultTask {

    public static final Set<String> FILES_NEEDED_FOR_LINKS = Set.of(
            "package-list", "element-list", "module-list"
    );
    
    @Nullable
    private final Javadoc baseTask;
    
    public JavadocLinksTask() {
        this(null);
    }
    
    @Inject
    public JavadocLinksTask(@Nullable Javadoc baseTask) {
        this.baseTask = baseTask;
        this.getNamespaces().convention(this.getProject().provider(ArrayList::new));
        this.getLinksDirectory().convention(TaskUtils.defaultOutputDirectory(this, "links"));
        this.getOptionFile().convention(TaskUtils.defaultOutputFile(this, "linkOptions.txt"));
    }

    @Input
    @Optional
    public abstract Property<URI> getConfig();

    @Input
    public abstract ListProperty<String> getNamespaces();
    
    @OutputDirectory
    public abstract DirectoryProperty getLinksDirectory();
    
    @OutputFile
    public abstract RegularFileProperty getOptionFile();
    
    public void config(String config) {
        try {
            this.getConfig().set(new URI(config));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI: " + config);
        }
    }
    
    public void namespace(String namespace) {
        this.getNamespaces().add(namespace);
    }

    @TaskAction
    protected void generateJavadocLinks(InputChanges inputs) throws IOException {
        Path optionFilePath = this.getOptionFile().get().getAsFile().toPath();
        Path linksPath = this.getLinksDirectory().get().getAsFile().toPath();

        PathUtils.createParentDirectories(optionFilePath);
        Files.createDirectories(linksPath);
        
        if (!this.getConfig().isPresent() || (this.baseTask != null && this.baseTask.getOptions().getDoclet() != null)) {
            // Either we have no config, or a custom doclet in which case we won't add any options.
            Files.writeString(optionFilePath, "\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return;
        }
        
        JavadocLinkData linkData = JavadocLinkData.read(this.getProject(), this.getConfig().get());
        List<String> includedNamespaces = this.getNamespaces().get().stream().sorted().toList();
        if (includedNamespaces.isEmpty()) includedNamespaces = linkData.listKnownNamespaces();
        
        try (Writer optionWriter = Files.newBufferedWriter(optionFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String namespaceId : includedNamespaces) {
                JavadocLinkData.Namespace namespace = linkData.getNamespace(namespaceId);
                for (JavadocLinkData.LinkResource resource : namespace.resources()) {
                    this.addResource(resource, linksPath, optionWriter);
                }
            }

            optionWriter.write("\n");
        }
    }
    
    private void addResource(JavadocLinkData.LinkResource resource, Path linksPath, Writer optionWriter) throws IOException {
        if (resource instanceof JavadocLinkData.RemoteLinkResource(URI remoteURI)) {
            optionWriter.write(" -link " + quote(remoteURI.toString()));
        } else if (resource instanceof JavadocLinkData.LocalLinkResource(URI remoteURI, java.net.URL downloadArtifact)) {
            Path targetPath = linksPath.resolve(downloadArtifact.toString().replace('/', '_').replace(':', '_'));
            if (Files.exists(targetPath)) PathUtils.deleteDirectory(targetPath);
            Files.createDirectories(targetPath);
            try (InputStream in = downloadArtifact.openStream(); ZipInputStream zin = new ZipInputStream(new BufferedInputStream(in))) {
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    String name = entry.getName().startsWith("/") ? entry.getName().substring(1) : entry.getName();
                    if (!entry.isDirectory() && FILES_NEEDED_FOR_LINKS.contains(name)) {
                        Path path = targetPath.resolve(name);
                        PathUtils.createParentDirectories(path);
                        Files.copy(zin, path);
                    }
                }
            }
            optionWriter.write(" -linkoffline " + quote(remoteURI.toString()) + " " + quote(targetPath.toAbsolutePath().normalize().toUri().toString()));
        } else {
            throw new IllegalArgumentException("I don't know how to process link resources of type " + resource.getClass().getName());
        }
    }

    public static String quote(String str) {
        return "\"" + str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace(" ", "\\ ") + "\"";
    }
}
