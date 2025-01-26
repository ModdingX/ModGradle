package org.moddingx.modgradle.plugins.javadoc;

import jakarta.annotation.Nonnull;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.MinimalJavadocOptions;
import org.moddingx.modgradle.ExternalDependencies;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.plugins.meta.MetaPlugin;
import org.moddingx.modgradle.plugins.meta.ModExtension;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class JavadocPlugin implements Plugin<Project> {
    
    @Override
    public void apply(@Nonnull Project project) {
        ModGradle.init(project);
        if (!project.getPlugins().hasPlugin("java-library")) {
            project.getPlugins().apply("java-library");
        }

        Configuration docletMetaConfiguration = project.getConfigurations().create("docletMetaClasspath", c -> {
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
        });
        project.getDependencies().add(docletMetaConfiguration.getName(), ExternalDependencies.JAVA_DOCLET_META.getDescriptor());

        // Copy to a list first to avoid ConcurrentModificationException
        for (Javadoc task : project.getTasks().withType(Javadoc.class).stream().toList()) {
            JavadocConfigureTask configureTask = project.getTasks().create(task.getName() + "Configure", JavadocConfigureTask.class);
            JavadocLinksTask linkTask = project.getTasks().create(task.getName() + "Links", JavadocLinksTask.class, task);
            Javadoc metaTask = project.getTasks().create(task.getName() + "Meta", Javadoc.class);

            configureTask.setGroup("documentation");
            linkTask.setGroup("documentation");
            metaTask.setGroup("documentation");
            
            task.dependsOn(configureTask);
            task.dependsOn(linkTask);
            metaTask.dependsOn(configureTask);
            task.finalizedBy(metaTask);
            
            project.afterEvaluate(p -> {
                metaTask.getJavadocTool().set(task.getJavadocTool());
                metaTask.setDestinationDir(new File(task.getDestinationDir(), "meta"));
                metaTask.setClasspath(task.getClasspath());
                metaTask.setSource(task.getSource()); // Otherwise we would skip with NO-SOURCE
                metaTask.options(o -> {
                    MinimalJavadocOptions originalOptions = task.getOptions();
                    o.destinationDirectory(new File(task.getDestinationDir(), "meta"));
                    o.setClasspath(originalOptions.getClasspath());
                    o.setDocletpath(docletMetaConfiguration.resolve().stream().toList());
                    o.setDoclet(ExternalDependencies.JAVA_DOCLET_META_DOCLET_CLASS);
                    o.optionFiles(configureTask.getDocletMetaOptionsFile().get().getAsFile());
                });

                task.options(o -> o.optionFiles(linkTask.getOptionFile().getAsFile().get()));
                task.doFirst(new SetupJavadocSourcesTaskAction(configureTask, false));
                metaTask.doFirst(new SetupJavadocSourcesTaskAction(configureTask, true));

                if (!linkTask.getConfig().isPresent() && project.getPlugins().hasPlugin(MetaPlugin.class)) {
                    ModExtension mod = project.getExtensions().findByType(ModExtension.class);
                    if (mod != null && mod.isCase("minecraft") && mod.getAt("minecraft") instanceof String minecraftVersion) {
                        try {
                            linkTask.getConfig().set(new URI("https://assets.moddingx.org/javadoc_links/" + minecraftVersion + ".json"));
                            if (linkTask.getNamespaces().get().isEmpty()) {
                                linkTask.getNamespaces().addAll("minecraft", "neoform", "neoforge");
                            }
                        } catch (URISyntaxException e) {
                            //
                        }
                    }
                }
            });
        }
    }

    private record SetupJavadocSourcesTaskAction(JavadocConfigureTask configureTask, boolean isMetaTask) implements Action<Task> {

        @Override
        public void execute(@Nonnull Task task) {
            if (task instanceof Javadoc javadoc) {
                try {
                    javadoc.setSource(this.isMetaTask() ? this.configureTask.getSources() : this.configureTask().getJavadocTaskInputs());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalStateException(this.getClass().getName() + " can only run on tasks of type Javadoc");
            }
        }
    }
}
