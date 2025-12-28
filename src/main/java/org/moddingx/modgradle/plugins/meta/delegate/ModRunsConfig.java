package org.moddingx.modgradle.plugins.meta.delegate;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import org.gradle.api.Action;

import java.util.ArrayList;
import java.util.List;

public class ModRunsConfig {
    
    public boolean autoConfig = true;
    public List<Action<Run>> client = new ArrayList<>();
    public List<Action<Run>> server = new ArrayList<>();
    public List<Action<Run>> clientData = new ArrayList<>();
    public List<Action<Run>> serverData = new ArrayList<>();
    public List<Action<Run>> gameTestServer = new ArrayList<>();
    
    public Delegate delegate() {
        return new Delegate();
    }
    
    public class Delegate {
        
        public void noAutoConfig() {
            ModRunsConfig.this.autoConfig = false;
        }
        
        public void all(@DelegatesTo(Run.class) Action<Run> closure) {
            ModRunsConfig.this.client.add(closure);
            ModRunsConfig.this.server.add(closure);
            ModRunsConfig.this.clientData.add(closure);
            ModRunsConfig.this.serverData.add(closure);
            ModRunsConfig.this.gameTestServer.add(closure);
        }
        
        public void client(@DelegatesTo(Run.class) Action<Run> closure) {
            ModRunsConfig.this.client.add(closure);
        }
        
        public void server(@DelegatesTo(Run.class) Action<Run> closure) {
            ModRunsConfig.this.server.add(closure);
        }
        
        public void data(@DelegatesTo(Run.class) Action<Run> closure) {
            ModRunsConfig.this.clientData.add(closure);
            ModRunsConfig.this.serverData.add(closure);
        }
        
        public void clientData(@DelegatesTo(Run.class) Action<Run> closure) {
            ModRunsConfig.this.clientData.add(closure);
        }
        
        public void serverData(@DelegatesTo(Run.class) Action<Run> closure) {
            ModRunsConfig.this.serverData.add(closure);
        }
        
        public void gameTestServer(@DelegatesTo(Run.class) Action<Run> closure) {
            ModRunsConfig.this.gameTestServer.add(closure);
        }
    }
}
