package org.moddingx.modgradle.plugins.meta.prop;

import jakarta.annotation.Nullable;
import org.moddingx.launcherlib.util.LazyValue;

public class ChangelogGenerationProperties {
    
    @Nullable private final String commitFormat;
    private final LazyValue<String> defaultChangelog;

    public ChangelogGenerationProperties(@Nullable String commitFormat, LazyValue<String> defaultChangelog) {
        this.commitFormat = commitFormat;
        this.defaultChangelog = defaultChangelog;
    }
    
    @Nullable 
    public String getCommitFormat() {
        return this.commitFormat;
    }

    public String getDefaultChangelog() {
        return this.defaultChangelog.get();
    }
}
