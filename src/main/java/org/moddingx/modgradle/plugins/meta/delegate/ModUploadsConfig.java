package org.moddingx.modgradle.plugins.meta.delegate;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import jakarta.annotation.Nullable;

import java.util.*;

public class ModUploadsConfig {
    
    public final CurseForgeUploadConfig curseforge = new CurseForgeUploadConfig();
    public final ModrinthUploadConfig modrinth = new ModrinthUploadConfig();
    
    public Delegate delegate() {
        return new Delegate();
    }
    
    public class Delegate {
        
        public void all(@DelegatesTo(value = ModUploadConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ModConfig.configure(closure, ModUploadsConfig.this.curseforge.delegate());
            ModConfig.configure(closure, ModUploadsConfig.this.modrinth.delegate());
        }
        
        public void curseforge(@DelegatesTo(value = CurseForgeUploadConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ModConfig.configure(closure, ModUploadsConfig.this.curseforge.delegate());
        }
        
        public void modrinth(@DelegatesTo(value = ModrinthUploadConfig.Delegate.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            ModConfig.configure(closure, ModUploadsConfig.this.modrinth.delegate());
        }
    }
    
    public static class ModUploadConfig {
        
        public ReleaseType type = ReleaseType.ALPHA;
        public boolean inferDefaultVersions = true;
        public final Set<String> versions = new HashSet<>();
        public final Set<String> requirements = new HashSet<>();
        public final Set<String> optionals = new HashSet<>();
        public String secretEnv;
        
        public class Delegate {
            
            public void type(String type) {
                try {
                    ModUploadConfig.this.type = ReleaseType.valueOf(type.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid release type: " + type, e);
                }
            }
            
            public void dontInferDefaultVersions() {
                ModUploadConfig.this.inferDefaultVersions = false;
            }
            
            public void version(String version) {
                ModUploadConfig.this.versions.add(version);
            }
            
            public void require(String requirement) {
                ModUploadConfig.this.requirements.add(requirement);
            }
            
            public void optional(String optional) {
                ModUploadConfig.this.optionals.add(optional);
            }
            
            public void secretEnv(String secretEnv) {
                ModUploadConfig.this.secretEnv = secretEnv;
            }
        }
    }
    
    public static class CurseForgeUploadConfig extends ModUploadConfig {
        
        public int projectId = 0;
        public final List<Closure<?>> cfgradle = new ArrayList<>();
        
        public CurseForgeUploadConfig() {
            this.secretEnv = "CURSEFORGE_UPLOAD_TOKEN";
        }
        
        public Delegate delegate() {
            return new Delegate();
        }
        
        public class Delegate extends ModUploadConfig.Delegate {
            
            public void projectId(int projectId) {
                CurseForgeUploadConfig.this.projectId = projectId;
            }
            
            public void cfgradle(Closure<?> closure) {
                CurseForgeUploadConfig.this.cfgradle.add(closure);
            }
        }
    }
    
    public static class ModrinthUploadConfig extends ModUploadConfig {
        
        @Nullable public String projectId = null;
        public final List<Closure<?>> minotaur = new ArrayList<>();

        public ModrinthUploadConfig() {
            this.secretEnv = "MODRINTH_UPLOAD_TOKEN";
        }
        
        public Delegate delegate() {
            return new Delegate();
        }
        
        public class Delegate extends ModUploadConfig.Delegate {
            
            public void projectId(String projectId) {
                ModrinthUploadConfig.this.projectId = projectId;
            }
            
            public void cfgradle(Closure<?> closure) {
                ModrinthUploadConfig.this.minotaur.add(closure);
            }
        }
    }
    
    public enum ReleaseType {
        ALPHA, BETA, RELEASE
    }
}
