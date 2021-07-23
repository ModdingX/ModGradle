package io.github.noeppi_noeppi.tools.modgradle.plugins.javadoc;

import io.github.noeppi_noeppi.tools.modgradle.util.task.DownloadTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.jvm.toolchain.JavadocTool;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public class JavadocPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        AtomicReference<DownloadTask> cssTask = new AtomicReference<>(null);
        project.getTasks().withType(Javadoc.class).forEach(jd -> {
            JavadocConfigureTask configureTask = project.getTasks().create(jd.getName() + "Configure", JavadocConfigureTask.class);
            jd.dependsOn(configureTask);
            JavadocLinksTask linkTask = project.getTasks().create(jd.getName() + "Links", JavadocLinksTask.class);
            jd.dependsOn(linkTask);
            if (cssTask.get() == null) {
                try {
                    cssTask.set(project.getTasks().create("javadocDownloadThemeCss", DownloadTask.class));
                    cssTask.get().redownload();
                    cssTask.get().setURL(new URL("https://gist.githubusercontent.com/noeppi-noeppi/56bb978ef90a61bbc749dee17d3ad98b/raw/darkmode.css"));
                    cssTask.get().setOutput(() -> project.file("build").toPath().resolve(cssTask.get().getName()).resolve("darkTheme.css").toFile());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to configure javadoc theme download task", e);
                }
            }
            jd.dependsOn(cssTask.get());
            project.afterEvaluate(p -> jd.options(o -> {
                o.optionFiles(configureTask.getOutput().getAsFile());
                o.optionFiles(linkTask.getOutput().getAsFile());
                JavadocTool tool = jd.getJavadocTool().getOrNull();
                if (tool != null && tool.getMetadata().getLanguageVersion().asInt() < 11) {
                    System.err.println("The used java version for " + jd.getName() + " does not support adding multiple stylesheets. Dark theme will not work.");
                } else {
                    if (o instanceof CoreJavadocOptions) {
                        ((CoreJavadocOptions) o).addFileOption("-add-stylesheet", cssTask.get().getOutput().getAsFile());
                    } else {
                        System.err.println("Failed to apply dark theme stylesheet to " + jd.getName() + ": Options are no CoreJavadocOptions");
                    }
                }
            }));
        });
    }
}
