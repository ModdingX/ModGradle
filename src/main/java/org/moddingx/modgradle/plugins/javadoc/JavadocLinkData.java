package org.moddingx.modgradle.plugins.javadoc;

import com.google.gson.*;
import org.gradle.api.Project;
import org.moddingx.modgradle.util.VfsUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavadocLinkData {
    
    private static final Gson GSON;
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        GSON = builder.create();
    }
    
    private final Map<String, Namespace> namespaces;

    public JavadocLinkData(Map<String, Namespace> namespaces) {
        this.namespaces = namespaces;
    }
    
    public List<String> listKnownNamespaces() {
        return this.namespaces.keySet().stream().sorted().toList();
    }
    
    public Namespace getNamespace(String name) {
        return this.namespaces.getOrDefault(name, new Namespace(List.of()));
    }

    public record Namespace(List<LinkResource> resources) {
        public Namespace(List<LinkResource> resources) {
            this.resources = List.copyOf(resources);
        }
    }
    
    public sealed interface LinkResource permits RemoteLinkResource, LocalLinkResource {
        URI remoteURI();
    }
    
    public record RemoteLinkResource(URI remoteURI) implements LinkResource { }
    public record LocalLinkResource(URI remoteURI, URL downloadArtifact) implements LinkResource { }
    
    public static JavadocLinkData read(Project project, URI resource) throws IOException {
        try (InputStream in = VfsUtils.open(project, resource); Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return read(reader);
        }
    }
    
    public static JavadocLinkData read(Reader reader) throws IOException {
        return read(GSON.fromJson(reader, JsonObject.class));
    }
    
    public static JavadocLinkData read(JsonObject json) throws IOException {
        try {
            Map<String, Namespace> namespaces = new HashMap<>();
            for (String namespaceId : json.keySet()) {
                JsonElement namespaceElement = json.get(namespaceId);
                if (!namespaceElement.isJsonArray() || !(namespaceElement instanceof JsonArray namespaceList)) {
                    throw new IOException("Invalid javadoc links: namespace must be an array.");
                }
                List<LinkResource> resources = new ArrayList<>();
                for (JsonElement resourceElement : namespaceList) {
                    if (!resourceElement.isJsonObject() || !(resourceElement instanceof JsonObject resourceObject)) {
                        throw new IOException("Invalid javadoc links: resource must be an object.");
                    }
                    if (!resourceObject.has("url") || !resourceObject.get("url").isJsonPrimitive()) {
                        throw new IOException("Invalid javadoc links: missing required property: url");
                    }
                    URI remoteURI = new URI(resourceObject.get("url").getAsString());
                    if (resourceObject.has("res")) {
                        URL downloadArtifact = new URI(resourceObject.get("res").getAsString()).toURL();
                        resources.add(new LocalLinkResource(remoteURI, downloadArtifact));
                    } else {
                        resources.add(new RemoteLinkResource(remoteURI));
                    }
                }
                namespaces.put(namespaceId, new Namespace(List.copyOf(resources)));
            }
            return new JavadocLinkData(Map.copyOf(namespaces));
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IOException("Invalid javadoc links: invalid URI", e);
        }
    }
}
