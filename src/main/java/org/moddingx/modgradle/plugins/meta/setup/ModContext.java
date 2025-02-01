package org.moddingx.modgradle.plugins.meta.setup;

import groovy.lang.Closure;
import jakarta.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.moddingx.launcherlib.util.LazyValue;
import org.moddingx.modgradle.ModGradle;
import org.moddingx.modgradle.plugins.meta.delegate.ModConfig;
import org.moddingx.modgradle.plugins.meta.delegate.ModVersionConfig;
import org.moddingx.modgradle.plugins.meta.prop.ChangelogGenerationProperties;
import org.moddingx.modgradle.util.GitChangelogGenerator;
import org.moddingx.modgradle.util.GitTagVersionResolver;
import org.moddingx.modgradle.util.GitTimestampUtils;
import org.moddingx.modgradle.util.MavenVersionResolver;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class ModContext extends ProjectContext {

    private final String modid;
    private final String group;
    private final String version;
    private final String minecraft;
    private final String neoforge;
    private final int java;
    private final int resource;
    private final int data;
    private final String license;
    @Nullable private final String timestamp;
    private final LazyValue<String> changelog;
    private final List<SourceSet> additionalModSources;

    public ModContext(ProjectContext context, ModConfig config) throws IOException {
        super(context);

        this.modid = Objects.requireNonNull(config.modid, "modid not set.");
        this.group = config.group != null ? config.group : this.project().getGroup().toString();
        this.version = resolveVersion(this.project(), this.group, config.version);
        this.neoforge = Objects.requireNonNull(config.neoforge, "neoforge version not set.");
        this.license = Objects.requireNonNull(config.license, "license not set.");
        this.minecraft = config.minecraft != null ? config.minecraft : ModGradle.versions().neoforgeMinecraft(this.project(), this.neoforge);
        this.java = config.java != 0 ? config.java : ModGradle.versions().java(this.minecraft);
        this.resource = ModGradle.versions().resource(this.minecraft);
        this.data = ModGradle.versions().data(this.minecraft);
        this.timestamp = GitTimestampUtils.tryGetCommitTimestamp(this.project());
        this.changelog = getChangelogProvider(this.project(), config.git.commitFormat, config.changelog);
        this.additionalModSources = config.additionalModSources.stream().distinct().toList();

        this.project().setGroup(this.group);
        this.project().setVersion(this.version);
        this.modProperty("modid", this.modid);
        this.modProperty("minecraft", this.minecraft);
        this.modProperty("neoforge", this.neoforge);
        this.modProperty("java", this.java);
        this.modProperty("resource", this.resource);
        this.modProperty("data", this.data);
        this.modProperty("license", this.license);
        if (this.timestamp != null) this.modProperty("timestamp", this.timestamp);
        this.modProperty("changelog", this.changelog);
    }

    private static String resolveVersion(Project project, String group, ModVersionConfig delegate) throws IOException {
        return switch (delegate.strategy) {
            case ModVersionConfig.Strategy.Constant.INSTANCE -> delegate.base == null ? project.getVersion().toString() : delegate.base;
            case ModVersionConfig.Strategy.Maven maven when delegate.base == null -> throw new IllegalStateException("Needs a base version to get version from a maven repository.");
            case ModVersionConfig.Strategy.Maven maven -> MavenVersionResolver.getVersion(project, maven.repository(), group, project.getName(), delegate.base);
            case ModVersionConfig.Strategy.GitTag.INSTANCE -> GitTagVersionResolver.getVersion(project, delegate.base);
        };
    }

    private static LazyValue<String> getChangelogProvider(Project project, @Nullable String commitFormat, @Nullable Closure<String> customChangelog) throws IOException {
        LazyValue<String> defaultChangelog = new LazyValue<>(() -> GitChangelogGenerator.generateChangelog(project, commitFormat));
        if (customChangelog == null) return defaultChangelog;
        return new LazyValue<>(() -> {
            ChangelogGenerationProperties properties = new ChangelogGenerationProperties(commitFormat, defaultChangelog);
            customChangelog.setDelegate(properties);
            customChangelog.setResolveStrategy(Closure.DELEGATE_FIRST);
            return customChangelog.call();
        });
    }

    public String modid() {
        return this.modid;
    }

    public String group() {
        return this.group;
    }

    public String version() {
        return this.version;
    }

    public String minecraft() {
        return this.minecraft;
    }

    public String neoforge() {
        return this.neoforge;
    }

    public int java() {
        return this.java;
    }

    public int resource() {
        return this.resource;
    }

    public int data() {
        return this.data;
    }

    public String license() {
        return this.license;
    }

    @Nullable
    public String timestamp() {
        return this.timestamp;
    }

    public String changelog() {
        return this.changelog.get();
    }

    public List<SourceSet> additionalModSources() {
        return this.additionalModSources;
    }
}
