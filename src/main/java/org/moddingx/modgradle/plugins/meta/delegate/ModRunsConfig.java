package org.moddingx.modgradle.plugins.meta.delegate;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import net.neoforged.gradle.dsl.common.runs.run.Run;

import java.util.ArrayList;
import java.util.List;

public class ModRunsConfig {
    
    public boolean autoConfig = true;
    public List<Closure<?>> client = new ArrayList<>();
    public List<Closure<?>> server = new ArrayList<>();
    public List<Closure<?>> data = new ArrayList<>();
    public List<Closure<?>> gameTestServer = new ArrayList<>();
    
    public Delegate delegate() {
        return new Delegate();
    }
    
    public class Delegate {
        
        public void noAutoConfig() {
            ModRunsConfig.this.autoConfig = false;
        }
        
        public void all(@DelegatesTo(Run.class) Closure<?> closure) {
            ModRunsConfig.this.client.add(closure);
            ModRunsConfig.this.server.add(closure);
            ModRunsConfig.this.data.add(closure);
            ModRunsConfig.this.gameTestServer.add(closure);
        }
        
        public void client(@DelegatesTo(Run.class) Closure<?> closure) {
            ModRunsConfig.this.client.add(closure);
        }
        
        public void server(@DelegatesTo(Run.class) Closure<?> closure) {
            ModRunsConfig.this.server.add(closure);
        }
        
        public void data(@DelegatesTo(Run.class) Closure<?> closure) {
            ModRunsConfig.this.data.add(closure);
        }
        
        public void gameTestServer(@DelegatesTo(Run.class) Closure<?> closure) {
            ModRunsConfig.this.gameTestServer.add(closure);
        }
    }
}
