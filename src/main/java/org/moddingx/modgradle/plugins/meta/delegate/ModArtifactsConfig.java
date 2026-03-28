package org.moddingx.modgradle.plugins.meta.delegate;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ModArtifactsConfig {

    @Nullable public ArtifactConfig sources = null;
    @Nullable public ArtifactConfig javadoc = null;
    public boolean useJarJar = false;
    public final List<ExtraArtifactConfig> extras = new ArrayList<>();

    public Delegate delegate() {
        return new Delegate();
    }

    public class Delegate {

        public void sources() {
            if (ModArtifactsConfig.this.sources == null) {
                ModArtifactsConfig.this.sources = new ArtifactConfig();
            }
        }
        
        public void sources(@DelegatesTo(value = ArtifactConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            if (ModArtifactsConfig.this.sources == null) {
                ModArtifactsConfig.this.sources = new ArtifactConfig();
            }
            ModConfig.configure(closure, ModArtifactsConfig.this.sources.delegate());
        }
        
        public void javadoc() {
            if (ModArtifactsConfig.this.javadoc == null) {
                ModArtifactsConfig.this.javadoc = new ArtifactConfig();
            }
        }
        
        public void javadoc(@DelegatesTo(value = ArtifactConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            if (ModArtifactsConfig.this.javadoc == null) {
                ModArtifactsConfig.this.javadoc = new ArtifactConfig();
            }
            ModConfig.configure(closure, ModArtifactsConfig.this.javadoc.delegate());
        }

        public void useJarJar() {
            ModArtifactsConfig.this.useJarJar = true;
        }

        public void extra(String taskName) {
            ModArtifactsConfig.this.extras.add(new ExtraArtifactConfig(taskName));
        }

        public void extra(String taskName, @DelegatesTo(value = ExtraArtifactConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ExtraArtifactConfig config = new ExtraArtifactConfig(taskName);
            ModArtifactsConfig.this.extras.add(config);
            ModConfig.configure(closure, config.delegate());
        }
    }

    public static class ArtifactConfig {

        public boolean publishToRepositories = false;
        public boolean uploadToModHostingSites = false;
        
        public Delegate delegate() {
            return new Delegate();
        }
        
        public class Delegate {
            
            public void publishToRepositories() {
                ArtifactConfig.this.publishToRepositories = true;
            }
            
            public void uploadToModHostingSites() {
                ArtifactConfig.this.uploadToModHostingSites = true;
            }
        }
    }

    public static class ExtraArtifactConfig {

        public final String taskName;
        @Nullable public String classifier = null;
        public boolean publishToRepositories = false;
        public boolean uploadToModHostingSites = false;

        public ExtraArtifactConfig(String taskName) {
            this.taskName = taskName;
        }

        public Delegate delegate() {
            return new Delegate();
        }

        public class Delegate {

            public void classifier(String classifier) {
                ExtraArtifactConfig.this.classifier = classifier;
            }

            public void publishToRepositories() {
                ExtraArtifactConfig.this.publishToRepositories = true;
            }

            public void uploadToModHostingSites() {
                ExtraArtifactConfig.this.uploadToModHostingSites = true;
            }
        }
    }
}
