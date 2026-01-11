package org.moddingx.modgradle.plugins.meta.delegate;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import jakarta.annotation.Nullable;

public class ModArtifactsConfig {
    
    @Nullable public ArtifactConfig sources = null;
    @Nullable public ArtifactConfig javadoc = null;
    public String mainJarTaskName = "jar";
    public String jarClassifier = "";
    public String jarJarClassifier = "all";

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
            this.useJarJar(false);
        }

        public void useJarJar(boolean switchClassifier) {
            ModArtifactsConfig.this.mainJarTaskName = "jarJar";
            if (switchClassifier) {
                ModArtifactsConfig.this.jarClassifier = "thin";
                ModArtifactsConfig.this.jarJarClassifier = "";
            }
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
}
