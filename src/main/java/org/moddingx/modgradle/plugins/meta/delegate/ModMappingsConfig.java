package org.moddingx.modgradle.plugins.meta.delegate;

import groovy.lang.Closure;
import jakarta.annotation.Nullable;
import org.moddingx.launcherlib.util.Artifact;

import java.util.Objects;

public class ModMappingsConfig {

    public Strategy strategy = new Strategy.Parchment(null, null, null);
    
    public Delegate delegate() {
        return new Delegate();
    }
    
    public sealed interface Strategy permits Strategy.Official, Strategy.Parchment {
        enum Official implements Strategy { INSTANCE }
        record Parchment(@Nullable String minecraft, @Nullable String version, @Nullable Artifact artifact) implements Strategy {}
    }
    
    public class Delegate {
        
        public void official() {
            ModMappingsConfig.this.strategy = Strategy.Official.INSTANCE;
        }
        
        public void parchment(String version) {
            ModMappingsConfig.this.strategy = new Strategy.Parchment(version, null, null);
        }
        
        public void parchment(Closure<?> closure) {
            class ParchmentDelegate {
                @Nullable private String minecraft = null;
                @Nullable private String version = null;
                @Nullable private Artifact artifact = null;

                public void minecraft(String minecraft) {
                    this.minecraft = Objects.requireNonNull(minecraft);
                    this.artifact = null;
                }

                public void version(String version) {
                    this.version = Objects.requireNonNull(version);
                    this.artifact = null;
                }

                public void artifact(String artifact) {
                    this.artifact = Artifact.from(artifact);
                    this.version = null;
                    this.minecraft = null;
                }
            }
            ParchmentDelegate delegate = ModConfig.configure(closure, new ParchmentDelegate());
            ModMappingsConfig.this.strategy = new Strategy.Parchment(delegate.minecraft, delegate.version, delegate.artifact);
        }
    }
}
