package io.github.noeppi_noeppi.tools.modgradle.plugins.sourcejar;

import io.github.noeppi_noeppi.tools.modgradle.api.Versioning;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaHelper;
import io.github.noeppi_noeppi.tools.modgradle.util.McEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.TaskUtil;
import io.github.noeppi_noeppi.tools.modgradle.util.task.ExtractInheritanceTask;
import io.github.noeppi_noeppi.tools.modgradle.util.task.MergeMappingsTask;
import io.github.noeppi_noeppi.tools.modgradle.util.task.SourceMappingsTask;
import net.minecraftforge.gradle.common.tasks.ApplyRangeMap;
import net.minecraftforge.gradle.common.tasks.ExtractRangeMap;
import net.minecraftforge.gradle.mcp.tasks.GenerateSRG;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Paths;
import java.util.Set;

// Plugin to build SRG named source jars
public class SourceJarPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        GenerateSRG generateMappings = TaskUtil.getOrNull(project, "createMcpToSrg", GenerateSRG.class);
        if (generateMappings == null) throw new IllegalStateException("The SourceJar plugin can't find the MCP -> SRG mappings.");
        
        JavaCompile compileTask = TaskUtil.getOrNull(project, "compileJava", JavaCompile.class);
        if (compileTask == null) {
            System.out.println("The SourceJar plugin was not able to find the `compileJava` task. You might need to configure stuff manually.");
        }
        
        Jar jarTask = TaskUtil.getOrNull(project, "jar", Jar.class);
        if (compileTask == null) {
            System.out.println("The SourceJar plugin was not able to find the `jar` task. You might need to configure stuff manually.");
        }
        
        Task reobfJarTask = TaskUtil.getOrNull(project, "reobfJar", Task.class);
        Task buildTask = TaskUtil.getOrNull(project, "build", Task.class);

        ExtractInheritanceTask extractInheritance = project.getTasks().create("sourceJarExtractInheritance", ExtractInheritanceTask.class);
        extractInheritance.getOutputs().upToDateWhen(t -> false);
        if (compileTask != null) extractInheritance.dependsOn(compileTask);
        SourceMappingsTask createSourceMappings = project.getTasks().create("sourceJarGenerateMappings", SourceMappingsTask.class);
        createSourceMappings.getOutputs().upToDateWhen(t -> false);
        createSourceMappings.dependsOn(extractInheritance, generateMappings);
        MergeMappingsTask mergeSourceMappings = project.getTasks().create("sourceJarMergeMappings", MergeMappingsTask.class);
        mergeSourceMappings.getOutputs().upToDateWhen(t -> false);
        mergeSourceMappings.dependsOn(generateMappings, createSourceMappings);
        ExtractRangeMap createRangeMap = project.getTasks().create("sourceJarRangeExtract", ExtractRangeMap.class);
        createRangeMap.getOutputs().upToDateWhen(t -> false);
        if (compileTask != null) createRangeMap.dependsOn(compileTask);
        ApplyRangeMap applyRangeMap = project.getTasks().create("sourceJarRangeApply", ApplyRangeMap.class);
        applyRangeMap.getOutputs().upToDateWhen(t -> false);
        applyRangeMap.dependsOn(createRangeMap, mergeSourceMappings);
        MergeJarWithSourcesTask mergeJars = project.getTasks().create("sourceJar", MergeJarWithSourcesTask.class);
        if (jarTask != null) mergeJars.dependsOn(jarTask);
        if (jarTask != null && reobfJarTask != null) mergeJars.dependsOn(reobfJarTask);
        mergeJars.dependsOn(applyRangeMap);
        if (buildTask != null) buildTask.dependsOn(mergeJars);
        project.afterEvaluate(p -> {
            Set<File> sources = JavaEnv.getJavaSources(project).getJava().getSrcDirs();
            FileCollection classpath = compileTask == null ? project.files() : compileTask.getClasspath();
            ConfigurableFileCollection libraryPath = project.files();
            libraryPath.from(classpath);
            if (compileTask != null) {
                if (compileTask.getJavaCompiler().getOrNull() != null) {
                    JavaHelper.addBuiltinLibraries(project, compileTask.getJavaCompiler().get().getMetadata().getInstallationPath().getAsFile().toPath(), libraryPath);
                } else {
                    JavaHelper.addBuiltinLibraries(project, Paths.get(System.getProperty("java.home")), libraryPath);
                }
            }
            String mcv = McEnv.findMinecraftVersion(project);
            int java = Versioning.getJavaVersion(mcv);
            
            if (compileTask != null) extractInheritance.setClasses(compileTask.getDestinationDirectory().get());
            extractInheritance.setLibraryPath(libraryPath);
            extractInheritance.setOutput(() -> project.file("build").toPath().resolve(extractInheritance.getName()).resolve("inheritance.txt").toFile());

            createSourceMappings.setInheritance(extractInheritance.getOutput());
            createSourceMappings.setMappings(generateMappings.getOutput().get());
            createSourceMappings.setOutput(() -> project.file("build").toPath().resolve(createSourceMappings.getName()).resolve("source_mappings.tsrg").toFile());
            
            mergeSourceMappings.setPrimary(generateMappings.getOutput().get());
            mergeSourceMappings.setMappings(project.files(createSourceMappings.getOutput()));
            mergeSourceMappings.setOutput(() -> project.file("build").toPath().resolve(mergeSourceMappings.getName()).resolve("source_mappings.tsrg").toFile());

            sources.forEach(createRangeMap.getSources()::from);
            createRangeMap.getDependencies().from(classpath);
            createRangeMap.getOutput().set(project.file("build").toPath().resolve(createRangeMap.getName()).resolve("rangemap.txt").toFile());
            if (java <= 8) {
                createRangeMap.getSourceCompatibility().set("JAVA_1_" + java);
            } else {
                createRangeMap.getSourceCompatibility().set("JAVA_" + java);
            }

            applyRangeMap.getSources().from(sources);
            applyRangeMap.getSrgFiles().from(mergeSourceMappings.getOutput());
            applyRangeMap.getRangeMap().set(createRangeMap.getOutput());
            applyRangeMap.getOutput().set(project.file("build").toPath().resolve(applyRangeMap.getName()).resolve("srg_sources.zip").toFile());
            
            if (jarTask != null) {
                mergeJars.setBase(jarTask.getArchiveFile().get());
                mergeJars.getDestinationDirectory().set(jarTask.getDestinationDirectory());
                mergeJars.getArchiveBaseName().set(jarTask.getArchiveBaseName());
                mergeJars.getArchiveAppendix().set(jarTask.getArchiveAppendix());
                mergeJars.getArchiveVersion().set(jarTask.getArchiveVersion());
                mergeJars.getArchiveExtension().set(jarTask.getArchiveExtension());
                mergeJars.getArchiveClassifier().set("sources");
            } else {
                mergeJars.getDestinationDirectory().set(project.file("build").toPath().resolve("libs").toFile());
            }
            mergeJars.setSources(applyRangeMap.getOutput().get());
        });
    }
}
