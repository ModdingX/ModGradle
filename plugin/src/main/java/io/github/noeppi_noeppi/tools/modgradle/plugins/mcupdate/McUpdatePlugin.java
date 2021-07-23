package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate;

import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.task.*;
import net.minecraftforge.gradle.common.tasks.ApplyRangeMap;
import net.minecraftforge.gradle.common.tasks.ExtractRangeMap;
import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.tasks.compile.JavaCompile;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

// Utilities to update projects from the version below the
// current version to the current version of minecraft.
public class McUpdatePlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        JavaCompile compileTask = project.getTasks().getByName("compileJava") instanceof JavaCompile jc ? jc : null;
        if (compileTask == null) {
            System.out.println("The mcupdate plugin was not able to find the `compileJava` task. You might need to configure stuff manually.");
        }
        
        BuildReMappingsTask remapNamed = project.getTasks().create("mcupdateMappings", BuildReMappingsTask.class);
        ExtractInheritanceTask extractInheritance = project.getTasks().create("mcupdate_extractInheritance", ExtractInheritanceTask.class);
        if (compileTask != null) extractInheritance.dependsOn(compileTask);
        DownloadTask downloadTransformer = project.getTasks().create("mcupdate_downloadTransformer", DownloadTask.class);
        downloadTransformer.redownload();
        ApplyTransformerTask applyTransformer = project.getTasks().create("mcupdate_applyTransformer", ApplyTransformerTask.class);
        applyTransformer.dependsOn(remapNamed, extractInheritance, downloadTransformer);
        MergeMappingsTask mergeSourceMappings = project.getTasks().create("mcupdate_mergeSourceMappings", MergeMappingsTask.class);
        mergeSourceMappings.dependsOn(remapNamed, applyTransformer);
        ExtractLocalRenameMapTask extractLocal = project.getTasks().create("mcupdate_extractLocal", ExtractLocalRenameMapTask.class);
        extractLocal.dependsOn(extractInheritance, mergeSourceMappings);
        CreateRenameCommentsTask stageLocal = project.getTasks().create("mcupdate_stageLocal", CreateRenameCommentsTask.class);
        stageLocal.dependsOn(extractLocal);
        ExtractRangeMap createRangeMap = project.getTasks().create("mcupdate_createRangeMap", ExtractRangeMap.class);
        createRangeMap.dependsOn(stageLocal);
        ApplyRangeMap remapSources = project.getTasks().create("mcupdate_remapSources", ApplyRangeMap.class);
        remapSources.dependsOn(mergeSourceMappings, createRangeMap);
        ExtractRemappedTask extractRemapped = project.getTasks().create("mcupdate_extractRemapped", ExtractRemappedTask.class);
        extractRemapped.dependsOn(remapSources);
        ApplyRenameCommentsTask applyLocal = project.getTasks().create("mcupdate_applyLocal", ApplyRenameCommentsTask.class);
        applyLocal.dependsOn(extractRemapped);
        RemapSrgSourcesTask remapSrg = project.getTasks().create("mcupdate_remapSrgSources", RemapSrgSourcesTask.class);
        remapSrg.dependsOn(applyLocal);
        UpdateMetaTask meta = project.getTasks().create("mcupdate_updateMeta", UpdateMetaTask.class);
        project.getTasks().create("mcupdate", DefaultTask.class).dependsOn(remapSrg, extractRemapped, meta);
        
        project.afterEvaluate(p -> {
            Set<File> sources = JavaEnv.getJavaSources(project).getJava().getSrcDirs();
            FileCollection classpath = compileTask == null ? project.files() : compileTask.getClasspath();
            ConfigurableFileCollection libraryPath = project.files();
            libraryPath.from(classpath);
            if (compileTask != null) {
                if (compileTask.getJavaCompiler().getOrNull() != null) {
                    this.addBuiltinLibraries(project, compileTask.getJavaCompiler().get().getMetadata().getInstallationPath().getAsFile().toPath(), libraryPath);
                } else {
                    this.addBuiltinLibraries(project, Paths.get(System.getProperty("java.home")), libraryPath);
                }
            }

            if (compileTask != null) extractInheritance.setClasses(compileTask.getDestinationDirectory().get());
            extractInheritance.setLibraryPath(libraryPath);
            extractInheritance.setOutput(() -> project.file("build").toPath().resolve(remapNamed.getName()).resolve("inheritance.txt").toFile());

            try {
                downloadTransformer.setURL(new URL("file:///home/tux/dev/util/MinecraftUtilities/mcupdate/1.17/transformer.json"));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            downloadTransformer.setOutput(() -> project.file("build").toPath().resolve(remapNamed.getName()).resolve("transformer.json").toFile());

            applyTransformer.setInheritance(extractInheritance.getOutput());
            applyTransformer.setTransformer(downloadTransformer.getOutput());
            applyTransformer.setMappings(remapNamed.getOutput());
            applyTransformer.setOutput(() -> project.file("build").toPath().resolve(remapNamed.getName()).resolve("additional.tsrg").toFile());

            mergeSourceMappings.setPrimary(remapNamed.getOutput());
            mergeSourceMappings.setMappings(project.files(applyTransformer.getOutput()));
            mergeSourceMappings.setOutput(() -> project.file("build").toPath().resolve(remapNamed.getName()).resolve("merged.tsrg").toFile());

            extractLocal.setInheritance(extractInheritance.getOutput());
            extractLocal.setMappings(mergeSourceMappings.getOutput());
            extractLocal.setTransformer(downloadTransformer.getOutput());
            if (!sources.isEmpty()) {
                File src = sources.iterator().next();
                extractLocal.setSources(project.getLayout().dir(new DefaultProvider<>(() -> src)).get());
                if (sources.size() > 1) {
                    System.out.println("Skipping " + (sources.size() - 1) + " source directories for local remapping, only using: " + src.toPath().toAbsolutePath().normalize().toString());
                }
            }
            extractLocal.setLibraryPath(libraryPath);
            extractLocal.setOutput(() -> project.file("build").toPath().resolve(remapNamed.getName()).resolve("local.txt").toFile());

            stageLocal.setSources(extractLocal.getSources());
            stageLocal.setRenameMap(extractLocal.getOutput());
            
            sources.forEach(createRangeMap.getSources()::from);
            createRangeMap.getDependencies().from(classpath);
            createRangeMap.getOutput().set(project.file("build").toPath().resolve("mcupdateMappings").resolve("rangemap.txt").toFile());

            remapSources.getSources().from(sources);
            remapSources.getSrgFiles().from(mergeSourceMappings.getOutput().getAsFile());
            remapSources.getRangeMap().set(createRangeMap.getOutput());
            remapSources.getOutput().set(project.file("build").toPath().resolve("mcupdateMappings").resolve("remapped.zip").toFile());

            extractRemapped.setInput(remapSources.getOutput().get());
            
            applyLocal.setSources(stageLocal.getSources());
        });
    }
    
    private void addBuiltinLibraries(Project project, Path javaHome, ConfigurableFileCollection libraryPath) {
        
        ConfigurableFileTree cpTreeJre = project.fileTree(javaHome.resolve("jre").resolve("lib"));
        cpTreeJre.include("*.jar");
        
        ConfigurableFileTree cpTreeJdk = project.fileTree(javaHome.resolve("lib"));
        cpTreeJdk.include("*.jar");
        
        ConfigurableFileTree mpTree = project.fileTree(javaHome.resolve("jmods"));
        mpTree.include("*.jmod");
        
        libraryPath.from(cpTreeJre, cpTreeJdk, mpTree);
    }
}
