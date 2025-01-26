package org.moddingx.modgradle.util;

import org.gradle.api.Project;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

public class VfsUtils {

    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    public static InputStream open(Project project, URI uri) throws IOException {
        return switch (uri.getScheme()) {
            case null -> Files.newInputStream(path(project, uri));
            case "", "file" -> Files.newInputStream(path(project, uri));
            case "http", "https" -> {
                try {
                    HttpResponse<Optional<InputStream>> response = HTTP.send(HttpRequest.newBuilder(uri).build(), resp -> {
                        if (resp.statusCode() == 404 || resp.statusCode() == 410) {
                            return HttpResponse.BodySubscribers.replacing(Optional.empty());
                        } else {
                            return HttpResponse.BodySubscribers.mapping(
                                    HttpUtils.successful(HttpResponse.BodyHandlers.ofInputStream()).apply(resp),
                                    Optional::of
                            );
                        }
                    });
                    Optional<InputStream> body = response.body();
                    if (body.isEmpty()) throw new NoSuchFileException(uri.toString());
                    yield body.get();
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
            default -> throw new IllegalArgumentException("Can't open URI scheme: " + uri);
        };
    }

    public static boolean exists(Project project, URI uri) throws IOException {
        return switch (uri.getScheme()) {
            case null -> Files.exists(path(project, uri));
            case "", "file" -> Files.exists(path(project, uri));
            case "http", "https" -> {
                try {
                    yield HTTP.send(HttpRequest.newBuilder(uri).build(), HttpUtils.resourceExists()).body();
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
            default -> throw new IllegalArgumentException("Can't open URI scheme: " + uri);
        };
    }

    private static Path path(Project project, URI uri) {
        if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
            return project.file(uri.getPath()).toPath();
        } else {
            return Path.of(uri);
        }
    }
}
