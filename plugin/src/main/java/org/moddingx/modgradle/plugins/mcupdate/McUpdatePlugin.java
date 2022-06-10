package org.moddingx.modgradle.plugins.mcupdate;

import com.google.gson.JsonObject;
import net.minecraftforge.gradle.common.tasks.ApplyRangeMap;
import net.minecraftforge.gradle.common.tasks.ExtractRangeMap;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.api.JavaEnvironment;
import org.moddingx.modgradle.api.task.DownloadTask;
import org.moddingx.modgradle.api.task.ExtractZipTask;
import org.moddingx.modgradle.api.task.MergeMappingsTask;
import org.moddingx.modgradle.plugins.mcupdate.task.*;
import org.moddingx.modgradle.util.JavaEnv;
import org.moddingx.modgradle.util.MgUtil;
import org.moddingx.modgradle.util.task.ExtractInheritanceTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Set;

public class McUpdatePlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project p) {
        ModGradle.initialiseProject(p);

        McUpdateExtension ext = p.getExtensions().create(McUpdateExtension.EXTENSION_NAME, McUpdateExtension.class);

        p.afterEvaluate(project -> {
            McUpdateData data = loadMcUpdateData(ext);

            Task remapSourcesTask = null;
            boolean hasMappings = data.mappings != null || !ext.getAdditionalMappings().isEmpty();
            boolean hasTransformer = data.transformer != null;
            if (hasMappings || hasTransformer) {
                remapSourcesTask = this.addSourceRemapTasks(project, ext, data, hasMappings, hasTransformer);
            }
            
            UpdateMetaTask updateMetaTask = project.getTasks().create("mcupdate_updateMeta", UpdateMetaTask.class);
            updateMetaTask.getMinecraft().set(ext.getMinecraft());
            if (remapSourcesTask != null) updateMetaTask.dependsOn(remapSourcesTask);
            
            Task finalTask = project.getTasks().create("mcupdate", DefaultTask.class);
            finalTask.getOutputs().upToDateWhen(t -> false);
            finalTask.dependsOn(updateMetaTask);
        });
    }
    
    private static McUpdateData loadMcUpdateData(McUpdateExtension ext) {
        try {
            URL url = ext.getConfig();
            Reader reader = new InputStreamReader(url.openStream());
            JsonObject json = ModGradle.GSON.fromJson(reader, JsonObject.class);
            reader.close();
            return new McUpdateData(json);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Mcupdate metadata not available for minecraft version " + ext.getMinecraft() + ":" + e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load mcupdate metadata: " + e);
        }
    }
    
    @Nullable
    private Task addSourceRemapTasks(Project project, McUpdateExtension ext, McUpdateData data, boolean hasMappings, boolean hasTransformer) {
        JavaCompile compileTask = MgUtil.task(project, "compileJava", JavaCompile.class);
        if (compileTask == null) {
            System.err.println("mcupdate: Skipping source remapping: compileJava task not found.");
            return null;
        } else {
            Provider<Directory> primarySourceDir = JavaEnv.getJavaSources(project).map(ss -> {
                Set<File> files = ss.getJava().getSrcDirs();
                if (files.isEmpty()) throw new IllegalStateException("main source set has no source directories.");
                return project.getLayout().dir(project.provider(() -> files.iterator().next())).get();
            });

            BuildBaseMappingsTask mappingTask = null;
            DownloadTask transformerTask = null;

            if (hasMappings) {
                mappingTask = project.getTasks().create("mcupdate_baseMappings", BuildBaseMappingsTask.class);
                if (data.mappings != null) mappingTask.getMainMappings().set(data.mappings);
                mappingTask.getAdditionalMappings().set(ext.getAdditionalMappings());
            }

            if (hasTransformer) {
                transformerTask = project.getTasks().create("mcupdate_downloadTransformer", DownloadTask.class);
                transformerTask.getUrl().set(data.transformer);
                transformerTask.getOutput().set(project.file("build").toPath().resolve(transformerTask.getName()).resolve("transformer.json").toFile());
                transformerTask.redownload();
            }

            ExtractInheritanceTask inheritanceTask = project.getTasks().create("mcupdate_extractInheritance", ExtractInheritanceTask.class);
            inheritanceTask.getTool().set(ext.getTool());
            inheritanceTask.getClasses().set(compileTask.getDestinationDirectory());
            inheritanceTask.getLibraryPath().set(JavaEnvironment.getLibraryPath(project, compileTask));
            inheritanceTask.dependsOn(compileTask);

            TransformTask transformTask = project.getTasks().create("mcupdate_transform", TransformTask.class);
            transformTask.getTool().set(ext.getTool());
            transformTask.getInheritance().set(inheritanceTask.getOutput());
            transformTask.dependsOn(inheritanceTask);
            if (mappingTask != null) {
                transformTask.getMappings().set(mappingTask.getMappingOutput());
                transformTask.dependsOn(mappingTask);
            }
            if (transformerTask != null) {
                transformTask.getTransformer().set(transformerTask.getOutput());
                transformTask.dependsOn(transformerTask);
            }

            Task nextDependencyTask = transformTask;
            Provider<RegularFile> finalMappings = transformTask.getOutput();
            if (mappingTask != null) {
                // Need to merge the additional mappings with the base mappings
                MergeMappingsTask mergeMappingsTask = project.getTasks().create("mcupdate_mergeMappings", MergeMappingsTask.class);
                mergeMappingsTask.getPrimary().set(mappingTask.getMappingOutput());
                mergeMappingsTask.getAdditional().set(project.provider(() -> project.files(transformTask.getOutput())));
                mergeMappingsTask.getNoParam().set(false);
                mergeMappingsTask.dependsOn(mappingTask, transformTask);
                nextDependencyTask = mergeMappingsTask;
                finalMappings = mergeMappingsTask.getOutput();
            }

            if (transformerTask != null) {
                ExtractLocalTask extractLocalTask = project.getTasks().create("mcupdate_extractLocal", ExtractLocalTask.class);
                extractLocalTask.getTool().set(ext.getTool());
                extractLocalTask.getInheritance().set(inheritanceTask.getOutput());
                extractLocalTask.getSources().set(JavaEnv.getJavaSourceDirs(project));
                extractLocalTask.getLibraryPath().set(JavaEnvironment.getLibraryPath(project, compileTask));
                extractLocalTask.getTransformer().set(transformerTask.getOutput());
                if (mappingTask != null) {
                    extractLocalTask.getMappings().set(mappingTask.getMappingOutput());
                }
                extractLocalTask.dependsOn(transformerTask, nextDependencyTask);

                StageLocalTask stageLocalTask = project.getTasks().create("mcupdate_stageLocal", StageLocalTask.class);
                stageLocalTask.getTool().set(ext.getTool());
                stageLocalTask.getSources().set(JavaEnv.getJavaSourceDirs(project));
                stageLocalTask.getRenameMap().set(extractLocalTask.getOutput());
                stageLocalTask.dependsOn(extractLocalTask);
                nextDependencyTask = stageLocalTask;
            }

            ExtractRangeMap rangeTask = project.getTasks().create("mcupdate_extractRange", ExtractRangeMap.class);
            rangeTask.getSources().from(JavaEnv.getJavaSourceDirs(project));
            rangeTask.getDependencies().from(compileTask.getClasspath());
            rangeTask.dependsOn(nextDependencyTask);
            nextDependencyTask = rangeTask;

            ApplyRangeMap remapTask = project.getTasks().create("mcupdate_applyRange", ApplyRangeMap.class);
            remapTask.getSources().from(JavaEnv.getJavaSourceDirs(project));
            remapTask.getSrgFiles().from(finalMappings);
            remapTask.getRangeMap().set(rangeTask.getOutput());
            remapTask.dependsOn(nextDependencyTask);
            nextDependencyTask = remapTask;

            ExtractZipTask extractRemapped = project.getTasks().create("mcupdate_extractRemapped", ExtractZipTask.class);
            extractRemapped.getInput().set(remapTask.getOutput());
            extractRemapped.getOutput().set(primarySourceDir);
            extractRemapped.doFirst(t -> {
                if (Files.exists(extractRemapped.getOutput().get().getAsFile().toPath())) {
                    try {
                        PathUtils.deleteDirectory(extractRemapped.getOutput().get().getAsFile().toPath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            extractRemapped.dependsOn(nextDependencyTask);
            nextDependencyTask = extractRemapped;

            if (hasTransformer) {
                ApplyRenameCommentsTask renameComments = project.getTasks().create("mcupdate_applyComments", ApplyRenameCommentsTask.class);
                renameComments.getSources().set(primarySourceDir);
                renameComments.dependsOn(nextDependencyTask);
                nextDependencyTask = renameComments;
            }

            return nextDependencyTask;
        }
    }
}
