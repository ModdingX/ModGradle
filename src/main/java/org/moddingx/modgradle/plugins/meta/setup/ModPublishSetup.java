package org.moddingx.modgradle.plugins.meta.setup;

import groovy.lang.Closure;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.moddingx.modgradle.plugins.meta.delegate.ModGitConfig;

import java.util.List;

public class ModPublishSetup {

    public static void configureBuild(ModContext mod, ModArtifactSetup.ConfiguredArtifacts artifacts, List<Closure<?>> repositories, ModGitConfig git) {
        if (!repositories.isEmpty()) {
            mod.project().getPlugins().apply("maven-publish");
            PublishingExtension ext = mod.project().getExtensions().getByType(PublishingExtension.class);
            ext.getPublications().create("mavenJava", MavenPublication.class, pub -> {
                pub.setGroupId(mod.group());
                pub.setArtifactId(mod.project().getName());
                pub.setVersion(mod.version());
                pub.artifact(artifacts.jar());
                if (artifacts.sources() != null && artifacts.sources().publish()) {
                    pub.artifact(artifacts.sources().task());
                }
                if (artifacts.javadoc() != null && artifacts.javadoc().publish()) {
                    pub.artifact(artifacts.javadoc().task());
                }
                pub.pom(pom -> {
                    pom.licenses(licenses -> licenses.license(license -> license.getName().set(mod.license())));
                    if (git.url != null || git.clone != null) {
                        pom.scm(scm -> {
                            if (git.url != null) scm.getUrl().set(git.url.toString());
                            if (git.clone != null) scm.getConnection().set("scm:git:" + git.clone);
                        });
                    }
                    if (git.issues != null) {
                        pom.issueManagement(issueManagement -> issueManagement.getUrl().set(git.issues.toString()));
                    }
                    pom.getProperties().put("modid", mod.modid());
                    pom.getProperties().put("minecraft_version", mod.minecraft());
                    pom.getProperties().put("neoforge_version", mod.neoforge());
                });
            });
            for (Closure<?> closure : repositories) {
                ext.getRepositories().configure(closure);
            }
        }
    }
}
