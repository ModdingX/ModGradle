package org.moddingx.modgradle.util;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.gradle.api.Project;
import org.moddingx.launcherlib.util.Artifact;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.OptionalLong;

public class MavenVersionResolver {

    public static String getVersion(Project project, URI repository, String group, String name, String baseVersion) throws IOException {
        URI repositoryMetadataUri = safeResolve(repository, group.replace('.', '/') + "/" + name + "/maven-metadata.xml");
        Metadata repositoryMetadata;
        try (InputStream metadata = VfsUtils.open(project, repositoryMetadataUri)) {
            repositoryMetadata = new MetadataXpp3Reader().read(metadata);
        } catch (NoSuchFileException | FileNotFoundException e) {
            // No metadata exists, treat empty
            repositoryMetadata = new Metadata();
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
        Versioning versioning = repositoryMetadata.getVersioning();
        if (versioning == null) versioning = new Versioning();
        OptionalLong mostRecentVersionPart = versioning.getVersions().stream()
                .<String>mapMulti((version, sink) -> {
                    if (version.startsWith(baseVersion + ".")) {
                        sink.accept(version.substring(baseVersion.length() + 1));
                    }
                })
                .flatMapToLong(versionPart -> versionPartNumber(versionPart).stream())
                .max();
        long nextVersionPart = mostRecentVersionPart.orElse(-1) + 1;
        Artifact nextVersion = Artifact.from(group, name, baseVersion + "." + nextVersionPart);
        checkThatVersionDoesNotExist(project, repository, nextVersion);
        return nextVersion.getVersion();
    }

    private static OptionalLong versionPartNumber(String versionPart) {
        int numericVersionPartLen = 0;
        while (versionPart.length() > numericVersionPartLen && "0123456789".indexOf(versionPart.charAt(numericVersionPartLen)) >= 0) {
            numericVersionPartLen += 1;
        }
        if (numericVersionPartLen == 0) return OptionalLong.empty();
        try {
            return OptionalLong.of(Long.parseLong(versionPart.substring(0, numericVersionPartLen)));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    public static void checkThatVersionDoesNotExist(Project project, URI repository, Artifact artifact) throws IOException {
        URI pomUri = safeResolve(repository, artifact.getPom().getPath());
        if (VfsUtils.exists(project, pomUri)) {
            throw new FileAlreadyExistsException("The project version " + artifact.getVersion() + " already exists in the repository. Is the maven metadata out of sync?");
        }
    }

    private static URI safeResolve(URI uri, String path) throws IOException {
        try {
            if (uri.getPath() == null) throw new IllegalArgumentException("I can't handle an opaque URI as maven repository");
            if (!uri.getPath().endsWith("/")) {
                uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath() + "/", uri.getQuery(), uri.getFragment());
            }
            while (path.startsWith("/")) path = path.substring(1);
            return uri.resolve(path);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
