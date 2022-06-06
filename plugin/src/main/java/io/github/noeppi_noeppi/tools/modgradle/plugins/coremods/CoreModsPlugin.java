package io.github.noeppi_noeppi.tools.modgradle.plugins.coremods;

import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import io.github.noeppi_noeppi.tools.modgradle.util.IOUtil;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaEnv;
import io.github.noeppi_noeppi.tools.modgradle.util.MgUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public class CoreModsPlugin implements Plugin<Project> {
    
    @Override
    public void apply(@Nonnull Project p) {
        ModGradle.initialiseProject(p);
        Configuration coreModsConfiguration = p.getConfigurations().create("coremods", c -> {
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
        });

        SourceSet coreModsSource = JavaEnv.getJavaExtension(p).get().getSourceSets().create("coremods", s -> {
            s.getJava().setSrcDirs(List.of());
            s.getResources().setSrcDirs(List.of(p.file("src/coremods")));
        });

        BuildCoreModsTask buildCoreMods = p.getTasks().create("buildCoreMods", BuildCoreModsTask.class);
        buildCoreMods.getCoreModSources().set(p.provider(() -> coreModsSource.getResources().getSourceDirectories()));
        PackCoreModsTask packCoreMods = p.getTasks().create("packCoreMods", PackCoreModsTask.class);
        packCoreMods.dependsOn(buildCoreMods);
        
        p.afterEvaluate(project -> {
            Path dep = resolveCoreModTypes(coreModsConfiguration);
            unzipTypesForIDE(coreModsSource, dep);
            
            buildCoreMods.getCoreModTypes().set(dep.toFile());
            packCoreMods.getCoreModTypes().set(dep.toFile());
            packCoreMods.getSourceDir().set(buildCoreMods.getOutputDir());

            Copy resourceTask = MgUtil.task(p, "processResources", Copy.class);
            if (resourceTask != null) {
                resourceTask.dependsOn(packCoreMods);
                resourceTask.from(packCoreMods.getTargetDir());
            }
        });
    }
    
    public static Path resolveCoreModTypes(Configuration coreModsConfiguration) {
        Set<File> files = coreModsConfiguration.resolve();
        if (files.size() != 1) throw new IllegalStateException("CoreMods configuration must declare exactly one dependency.");
        return files.iterator().next().toPath().toAbsolutePath().normalize();
    }
    
    public static void unzipTypesForIDE(SourceSet sourceSet, Path path) {
        try (FileSystem fs = IOUtil.getFileSystem(URI.create("jar:" + path.toUri()))) {
            Path typeSrc = fs.getPath("coremods.d.ts");
            Path cfgSrc = fs.getPath("tsconfig.json");
            for (File dir : sourceSet.getResources().getSrcDirs()) {
                Files.createDirectories(dir.toPath());
                Path typeDest = dir.toPath().toAbsolutePath().normalize().resolve("coremods.d.ts");
                Path cfgDest = dir.toPath().toAbsolutePath().normalize().resolve("tsconfig.json");
                Files.copy(typeSrc, typeDest, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(cfgSrc, cfgDest, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static List<Path> getRelativeCoreModPaths(Path path) throws IOException {
        try (Stream<Path> paths = Files.walk(path)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ts"))
                    .filter(p -> !p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".d.ts"))
                    .map(p -> path.relativize(p.toAbsolutePath()))
                    .toList();
        }
    }
}
