package org.moddingx.modgradle.plugins.packdev;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.moddingx.modgradle.plugins.packdev.api.CurseProperties;
import org.moddingx.modgradle.plugins.packdev.platform.ModFile;
import org.moddingx.modgradle.plugins.packdev.platform.ModdingPlatform;
import org.moddingx.modgradle.plugins.packdev.platform.curse.CursePlatform;
import org.moddingx.modgradle.plugins.packdev.platform.modrinth.ModrinthPlatform;
import org.moddingx.modgradle.plugins.packdev.target.CursePack;
import org.moddingx.modgradle.plugins.packdev.target.ModrinthPack;
import org.moddingx.modgradle.plugins.packdev.target.MultiMcPack;
import org.moddingx.modgradle.plugins.packdev.target.ServerPack;
import org.moddingx.modgradle.util.MgUtil;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackDevRegistry {
    
    private static final Map<String, ModdingPlatform<?>> platforms = new HashMap<>();
    private static final Map<String, ConfiguredTarget> targets = new HashMap<>();
    
    static {
        registerPlatform(CursePlatform.INSTANCE);
        registerPlatform(ModrinthPlatform.INSTANCE);
        
        registerTarget("curse", CursePack.class, CurseProperties.class);
        registerTarget("modrinth", ModrinthPack.class);
        registerTarget("server", ServerPack.class);
        registerTarget("multimc", MultiMcPack.class);
    }
    
    public static synchronized void registerPlatform(ModdingPlatform<?> platform) {
        String id = platform.id();
        if (platforms.containsKey(id)) {
            throw new IllegalStateException("Duplicate modding platform: " + id);
        } else {
            platforms.put(id, platform);
        }
    }
    
    public static synchronized void registerTarget(String id, Class<? extends Task> taskClass) {
        registerTarget(id, taskClass, null);
    }
    
    public static synchronized void registerTarget(String id, Class<? extends Task> taskClass, Class<?> propertiesClass) {
        if (targets.containsKey(id)) {
            throw new IllegalStateException("Duplicate modpack target: " + id);
        } else {
            targets.put(id, new ConfiguredTarget(taskClass, propertiesClass));
        }
    }
    
    public static synchronized ModdingPlatform<?> getPlatform(String id) {
        if (platforms.containsKey(id)) {
            return platforms.get(id);
        } else {
            throw new IllegalArgumentException("Unknown modding platform: PackDev can't handle modpacks for platform: " + id);
        }
    }
    
    public static synchronized Task createTargetTask(Project project, String id, ModdingPlatform<?> platform, PackSettings settings, List<? extends ModFile> files, @Nullable Object properties) {
        if (targets.containsKey(id)) {
            ConfiguredTarget target = targets.get(id);
            if (properties == null && target.propertiesClass() != null) {
                throw new IllegalArgumentException("Missing properties for target " + id + ", expected " + target.propertiesClass());
            } else if (target.propertiesClass() != null && !target.propertiesClass().isAssignableFrom(properties.getClass())) {
                throw new IllegalArgumentException("Invalid properties for target " + id + ", expected " + target.propertiesClass() + " got " + properties.getClass());
            } else if (target.propertiesClass() != null) {
                return project.getTasks().create("build" + MgUtil.capitalize(id) + "Pack", target.taskClass(), platform, settings, List.copyOf(files), properties);
            } else {
                return project.getTasks().create("build" + MgUtil.capitalize(id) + "Pack", target.taskClass(), platform, settings, List.copyOf(files));
            }
        } else {
            throw new IllegalArgumentException("Unknown modpack target: PackDev can't build targets of type: " + id);
        }
    }
    
    private record ConfiguredTarget(Class<? extends Task> taskClass, @Nullable Class<?> propertiesClass) {}
}
