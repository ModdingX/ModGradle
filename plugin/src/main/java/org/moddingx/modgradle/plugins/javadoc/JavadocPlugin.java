package org.moddingx.modgradle.plugins.javadoc;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.external.javadoc.MinimalJavadocOptions;
import org.gradle.jvm.toolchain.JavadocTool;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.api.task.DownloadTask;
import org.moddingx.modgradle.util.ConfigurationDownloader;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public class JavadocPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        ModGradle.initialiseProject(project);
        AtomicReference<DownloadTask> cssTask = new AtomicReference<>(null);
        AtomicReference<FileCollection> docletMetaFiles = new AtomicReference<>(null);
        // Copy to a list first to avoid ConcurrentModificationException
        project.getTasks().withType(Javadoc.class).stream().toList().forEach(jd -> {
            JavadocConfigureTask configureTask = project.getTasks().create(jd.getName() + "Configure", JavadocConfigureTask.class);
            jd.dependsOn(configureTask);
            JavadocLinksTask linkTask = project.getTasks().create(jd.getName() + "Links", JavadocLinksTask.class, jd);
            jd.dependsOn(linkTask);
            Javadoc jdMeta = project.getTasks().create(jd.getName() + "Meta", Javadoc.class);
            jdMeta.dependsOn(configureTask);
            jd.finalizedBy(jdMeta);
            
            if (cssTask.get() == null) {
                try {
                    cssTask.set(project.getTasks().create("javadocDownloadThemeCss", DownloadTask.class));
                    cssTask.get().redownload();
                    cssTask.get().getUrl().set(new URL("https://assets.moddingx.org/javadoc/darkTheme.css"));
                    cssTask.get().getOutput().set(project.file("build").toPath().resolve(cssTask.get().getName()).resolve("darkTheme.css").toFile());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to configure javadoc theme download task", e);
                }
            }
            jd.dependsOn(cssTask.get());
            
            project.afterEvaluate(p -> {
                if (docletMetaFiles.get() == null) {
                    docletMetaFiles.set(ConfigurationDownloader.download(project, ModGradle.DOCLET_META));
                }
                FileCollection resolvedDocletMetaFiles = docletMetaFiles.get();
                jd.options(o -> {
                    //noinspection Convert2Lambda
                    jd.doFirst(new Action<>() {
                        @Override
                        public void execute(@Nonnull Task t) {
                            FileCollection fc = project.files(configureTask.getSources().get().getFiles().stream()
                                    .map(File::toPath)
                                    .map(path -> path.toAbsolutePath().normalize())
                                    .map(configureTask::getDirs)
                                    .toArray());
                            jd.setSource(fc);

                            JavadocTool tool = jd.getJavadocTool().getOrNull();
                            if (tool != null && tool.getMetadata().getLanguageVersion().asInt() < 11) {
                                System.err.println("The used java version for " + jd.getName() + " does not support adding multiple stylesheets. Dark theme will not work.");
                            } else {
                                if (o instanceof CoreJavadocOptions co) {
                                    co.addFileOption("-add-stylesheet", cssTask.get().getOutput().get().getAsFile());
                                } else {
                                    System.err.println("Failed to apply dark theme stylesheet to " + jd.getName() + ": Options are no CoreJavadocOptions");
                                }
                            }
                        }
                    });
                    o.optionFiles(linkTask.getOutput().get().getAsFile());
                });
                
                jdMeta.getJavadocTool().set(jd.getJavadocTool());
                jdMeta.setDestinationDir(new File(jd.getDestinationDir(), "meta"));
                jdMeta.setClasspath(jd.getClasspath());
                jdMeta.options(o -> {
                    MinimalJavadocOptions mjo = jd.getOptions();
                    o.destinationDirectory(new File(jd.getDestinationDir(), "meta"));
                    o.setClasspath(mjo.getClasspath());
                    o.setDocletpath(resolvedDocletMetaFiles.getFiles().stream().toList());
                    o.setDoclet("org.moddingx.java_doclet_meta.Main");
                    o.optionFiles(configureTask.getDocletMetaOptions().get().getAsFile());
                });
                
                jdMeta.options(o -> {
                    //noinspection Convert2Lambda
                    jdMeta.doFirst(new Action<>() {
                        @Override
                        public void execute(@Nonnull Task t) {
                            // Exclude logic is done through options as JavaDocletMeta still needs full
                            // access to all the classes.
                            // However, we need to set the sources here as these are no properties yet, so
                            // changes in the configure-task would not reflect here.
                            jdMeta.setSource(configureTask.getSources().get());
                        }
                    });
                });
                
                // Set dummy sources to meta task, so they don't skip with NO-SOURCE
                jdMeta.setSource(jd.getSource());
            });
        });
    }
}
