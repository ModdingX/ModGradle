package io.github.noeppi_noeppi.tools.modgradle.plugins.sourcejar;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.api.Versioning;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaHelper;
import io.github.noeppi_noeppi.tools.modgradle.util.McEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.MgUtil;
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
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

// Plugin to build SRG named source jars
public class SourceJarPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        ModGradle.initialiseProject(project);
        GenerateSRG generateMappings = MgUtil.task(project, "createMcpToSrg", GenerateSRG.class);
        if (generateMappings == null) throw new IllegalStateException("The SourceJar plugin can't find the MCP -> SRG mappings.");
        
        JavaCompile compileTask = MgUtil.task(project, "compileJava", JavaCompile.class);
        if (compileTask == null) {
            System.out.println("The SourceJar plugin was not able to find the `compileJava` task. You might need to configure stuff manually.");
        }
        
        Jar jarTask = MgUtil.task(project, "jar", Jar.class);
        if (compileTask == null) {
            System.out.println("The SourceJar plugin was not able to find the `jar` task. You might need to configure stuff manually.");
        }
        
        Task reobfJarTask = MgUtil.task(project, "reobfJar", Task.class);
        Task buildTask = MgUtil.task(project, "build", Task.class);

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
            Set<File> sources = new HashSet<>(JavaEnv.getJavaSources(project).get().getJava().getSrcDirs());
            
            Provider<FileCollection> classpath = project.provider(() -> compileTask == null ? project.files() : compileTask.getClasspath());
            Provider<FileCollection> libraryPath = classpath.map(cp -> {
                ConfigurableFileCollection fc = project.files();
                fc.from(cp);
                if (compileTask != null) {
                    if (compileTask.getJavaCompiler().getOrNull() != null) {
                        JavaHelper.addBuiltinLibraries(project, compileTask.getJavaCompiler().get().getMetadata().getInstallationPath().getAsFile().toPath(), fc);
                    } else {
                        JavaHelper.addBuiltinLibraries(project, Paths.get(System.getProperty("java.home")), fc);
                    }
                }
                return fc;
            });
            
            Provider<String> mcv = McEnv.findMinecraftVersion(project);
            Provider<Integer> java = mcv.map(Versioning::getJavaVersion);
            
            if (compileTask != null) extractInheritance.getClasses().set(compileTask.getDestinationDirectory());
            extractInheritance.getLibraryPath().set(libraryPath);
            extractInheritance.getOutput().set(project.file("build").toPath().resolve(extractInheritance.getName()).resolve("inheritance.txt").toFile());

            createSourceMappings.getInheritance().set(extractInheritance.getOutput());
            createSourceMappings.getMappings().set(generateMappings.getOutput());
            createSourceMappings.getOutput().set(project.file("build").toPath().resolve(createSourceMappings.getName()).resolve("source_mappings.tsrg").toFile());
            
            mergeSourceMappings.getPrimary().set(generateMappings.getOutput());
            mergeSourceMappings.getMappings().set(project.files(createSourceMappings.getOutput()));
            mergeSourceMappings.getOutput().set(project.file("build").toPath().resolve(mergeSourceMappings.getName()).resolve("source_mappings.tsrg").toFile());
            mergeSourceMappings.getNoParam().set(true);

            sources.forEach(createRangeMap.getSources()::from);
            createRangeMap.getDependencies().from(classpath);
            createRangeMap.getOutput().set(project.file("build").toPath().resolve(createRangeMap.getName()).resolve("rangemap.txt").toFile());
            createRangeMap.getSourceCompatibility().set(java.map(jv -> jv <= 8 ? "JAVA_1_" + jv : "JAVA_" + jv));
            createRangeMap.setRuntimeJavaToolchain(JavaEnv.getJavaExtension(project).get().getToolchain());

            applyRangeMap.getSources().from(sources);
            applyRangeMap.getSrgFiles().from(mergeSourceMappings.getOutput());
            applyRangeMap.getRangeMap().set(createRangeMap.getOutput());
            applyRangeMap.getOutput().set(project.file("build").toPath().resolve(applyRangeMap.getName()).resolve("srg_sources.zip").toFile());
            applyRangeMap.setRuntimeJavaToolchain(JavaEnv.getJavaExtension(project).get().getToolchain());
            
            if (jarTask != null) {
                mergeJars.getBase().set(jarTask.getArchiveFile());
                mergeJars.getDestinationDirectory().set(jarTask.getDestinationDirectory());
                mergeJars.getArchiveBaseName().set(jarTask.getArchiveBaseName());
                mergeJars.getArchiveAppendix().set(jarTask.getArchiveAppendix());
                mergeJars.getArchiveVersion().set(jarTask.getArchiveVersion());
                mergeJars.getArchiveExtension().set(jarTask.getArchiveExtension());
                mergeJars.getArchiveClassifier().set("sources");
            } else {
                mergeJars.getDestinationDirectory().set(project.file("build").toPath().resolve("libs").toFile());
            }
            mergeJars.getSources().set(applyRangeMap.getOutput());
        });
    }
}
