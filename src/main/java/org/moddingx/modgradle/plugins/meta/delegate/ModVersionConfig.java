package org.moddingx.modgradle.plugins.meta.delegate;

import jakarta.annotation.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class ModVersionConfig {

    @Nullable public String base;
    @Nullable public String suffix;
    public Strategy strategy = Strategy.Constant.INSTANCE;

    public Delegate delegate() {
        return new Delegate();
    }
    
    public sealed interface Strategy permits Strategy.Constant, Strategy.Maven, Strategy.GitTag {
        enum Constant implements Strategy { INSTANCE }
        record Maven(URI repository) implements Strategy {}
        enum GitTag implements Strategy { INSTANCE }
    }
    
    public class Delegate {
        
        public void constant(String version) {
            ModVersionConfig.this.base = Objects.requireNonNull(version);
            ModVersionConfig.this.strategy = Strategy.Constant.INSTANCE;
        }

        public void base(String version) {
            ModVersionConfig.this.base = Objects.requireNonNull(version);
        }

        public void suffix(String suffix) {
            ModVersionConfig.this.suffix = Objects.requireNonNull(suffix);
        }
        
        public void maven(String repository) throws URISyntaxException {
            this.maven(new URI(repository));
        }
        
        public void maven(URI repository) {
            ModVersionConfig.this.strategy = new Strategy.Maven(Objects.requireNonNull(repository));
        }

        public void gitTag() {
            ModVersionConfig.this.strategy = Strategy.GitTag.INSTANCE;
        }
    }
}
