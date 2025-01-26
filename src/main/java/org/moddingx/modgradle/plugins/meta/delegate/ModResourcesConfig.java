package org.moddingx.modgradle.plugins.meta.delegate;

import java.util.HashSet;
import java.util.Set;

public class ModResourcesConfig {
    
    public Set<String> expandingPatterns = new HashSet<>(Set.of(
            "META-INF/neoforge.mods.toml"
    ));
    
    public Delegate delegate() {
        return new Delegate();
    }
    
    public class Delegate {

        public void noExpand() {
            ModResourcesConfig.this.expandingPatterns.clear();
        }

        public void expandIn(String pattern) {
            ModResourcesConfig.this.expandingPatterns.add(pattern);
        }
    }
}
