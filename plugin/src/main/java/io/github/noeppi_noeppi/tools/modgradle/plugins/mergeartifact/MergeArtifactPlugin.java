package io.github.noeppi_noeppi.tools.modgradle.plugins.mergeartifact;

import io.github.noeppi_noeppi.tools.modgradle.plugins.javadoc.JavadocConfigureTask;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.TaskUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.jvm.tasks.Jar;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;

// Merge an artifact in a built mod
// Merges service files and handles source jar and javadoc configuration
public class MergeArtifactPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project p) {
        p.getExtensions().create(MergeArtifactExtension.EXTENSION_NAME, MergeArtifactExtension.class);
        p.afterEvaluate(project -> {
            Jar jar = TaskUtil.getOrNull(project, "jar", Jar.class);
            if (jar == null) throw new IllegalStateException("Can't merge artifacts: jar task not found");
            MergedArtifacts.getShadeFiles(project).forEach(jar::from);
            File mergedServices = MergedArtifacts.mergeServiceFiles(project, JavaEnv.getJavaResourceDirs(project));
            jar.from(project.fileTree(mergedServices));
            // Exclude all services except from the merged file tree
            Path comparePath = mergedServices.toPath().toAbsolutePath().normalize();
            jar.exclude(fte -> String.join("/", fte.getRelativePath().getSegments()).startsWith("META-INF/services") && !fte.getFile().toPath().toAbsolutePath().normalize().startsWith(comparePath));
        
            Javadoc javadoc = TaskUtil.getOrNull(project, "javadoc", Javadoc.class);
            if (javadoc == null) throw new IllegalStateException("Can't merge artifacts: javadoc task not found");
            MergedArtifacts.additionalSourceDirs(project).stream().map(project::fileTree).forEach(javadoc::source);
            JavadocConfigureTask javadocConfigure = TaskUtil.getOrNull(project, "javadocConfigure", JavadocConfigureTask.class);
            if (javadocConfigure != null) {
                ConfigurableFileCollection files = project.files(javadocConfigure.getSources());
                // No file tree, the javadoc plugin operates on source dirs
                MergedArtifacts.additionalSourceDirs(project).forEach(files::from);
                javadocConfigure.setSources(files);
            }
        });
    }
}
