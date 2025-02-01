package org.moddingx.modgradle.plugins.meta.delegate;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import jakarta.annotation.Nullable;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.tasks.SourceSet;
import org.moddingx.modgradle.plugins.meta.prop.ChangelogGenerationProperties;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ModConfig {
    
    @Nullable public String modid = null;
    @Nullable public String group = null;
    @Nullable public String neoforge = null;
    @Nullable public String license = null;
    @Nullable public String minecraft = null;
    @Nullable public Closure<String> changelog = null;
    public int java = 0;
    
    public final List<SourceSet> additionalModSources = new ArrayList<>();
    public final ModVersionConfig version = new ModVersionConfig();
    public final ModGitConfig git = new ModGitConfig();
    public final ModMappingsConfig mappings = new ModMappingsConfig();
    public final ModArtifactsConfig artifacts = new ModArtifactsConfig();
    public final ModResourcesConfig resources = new ModResourcesConfig();
    public final ModRunsConfig runs = new ModRunsConfig();
    public final List<Closure<?>> publishing = new ArrayList<>();
    public final ModUploadsConfig upload = new ModUploadsConfig();

    public static <T> T configure(Closure<?> closure, T delegate) {
        return configure(closure, delegate, d -> d);
    }
    
    public static <T> T configure(Closure<?> closure, T object, Function<T, Object> delegate) {
        closure.setDelegate(delegate.apply(object));
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.run();
        return object;
    }

    public Delegate delegate() {
        return new Delegate();
    }

    public class Delegate {
        
        public void modid(String modid) {
            ModConfig.this.modid = Objects.requireNonNull(modid);
        }

        public void group(String group) {
            ModConfig.this.group = Objects.requireNonNull(group);
        }

        public void version(String version) {
            ModConfig.this.version.delegate().constant(version);
        }
        
        public void versioning(@DelegatesTo(value = ModVersionConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ModConfig.configure(closure, ModConfig.this.version.delegate());
        }

        public void git(@DelegatesTo(value = ModGitConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ModConfig.configure(closure, ModConfig.this.git.delegate());
        }

        public void github(String repo) throws URISyntaxException {
            ModConfig.this.git.delegate().github(repo);
        }

        public void gitlab(String repo) throws URISyntaxException {
            ModConfig.this.git.delegate().gitlab(repo);
        }

        public void neoforge(String neoforge) {
            ModConfig.this.neoforge = Objects.requireNonNull(neoforge);
        }

        public void license(String license) {
            ModConfig.this.license = Objects.requireNonNull(license);
        }

        public void minecraft(String minecraft) {
            ModConfig.this.minecraft = Objects.requireNonNull(minecraft);
        }

        public void java(int java) {
            ModConfig.this.java = java;
        }
        
        public void extraModSource(SourceSet... sourceSets) {
            ModConfig.this.additionalModSources.addAll(Arrays.asList(sourceSets));
        }
        
        public void mappings(@DelegatesTo(value = ModMappingsConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ModConfig.configure(closure, ModConfig.this.mappings.delegate());
        }
        
        public void changelog(@DelegatesTo(value = ChangelogGenerationProperties.class, strategy = Closure.DELEGATE_FIRST) Closure<String> closure) {
            ModConfig.this.changelog = Objects.requireNonNull(closure);
        }
        
        public void artifacts(@DelegatesTo(value = ModArtifactsConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ModConfig.configure(closure, ModConfig.this.artifacts.delegate());
        }
        
        public void resources(@DelegatesTo(value = ModResourcesConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ModConfig.configure(closure, ModConfig.this.resources.delegate());
        }

        public void runs(@DelegatesTo(value = ModRunsConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ModConfig.configure(closure, ModConfig.this.runs.delegate());
        }

        public void publishing(@DelegatesTo(RepositoryHandler.class) Closure<?> closure) {
            ModConfig.this.publishing.add(closure);
        }
        
        public void upload(@DelegatesTo(value = ModUploadsConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ModConfig.configure(closure, ModConfig.this.upload.delegate());
        }
    }
}
