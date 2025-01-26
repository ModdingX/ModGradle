package org.moddingx.modgradle.util;

import com.google.gson.*;
import org.apache.commons.io.file.PathUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.moddingx.launcherlib.launcher.Launcher;
import org.moddingx.launcherlib.util.Artifact;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModGradleVersionAccess {

    public static void main(String[] args) {
        ModGradleVersionAccess versions = new ModGradleVersionAccess(Path.of("/tmp/mgc/versions.json"), new Launcher(Path.of("/tmp/mgc/launcher")));
        System.out.println(versions.java("1.21.1"));
        System.out.println(versions.resource("1.21.1"));
        System.out.println(versions.data("1.21.1"));
    }

    private static final Gson GSON;
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        GSON = builder.create();
    }

    private final Path path;
    private final Launcher launcher;
    private final Map<String, String> neoforgeMinecraftVersions;
    private final Map<String, Integer> javaVersions; // For offline use, LauncherLib doesn't cache these
    private final Map<String, PackVersions> packVersions;

    public ModGradleVersionAccess(Path path, Launcher launcher) {
        this.path = path;
        this.launcher = launcher;
        this.neoforgeMinecraftVersions = new HashMap<>();
        this.javaVersions = new HashMap<>();
        this.packVersions = new HashMap<>();
        this.load();
    }

    public String neoforgeMinecraft(Project project, String neoforge) {
        if (this.neoforgeMinecraftVersions.containsKey(neoforge)) return this.neoforgeMinecraftVersions.get(neoforge);
        try {
            Artifact neoforgeUniversal = Artifact.from("net.neoforged", "neoforge", neoforge, "universal");
            ModuleDependency neoforgeDependency = (ModuleDependency) project.getDependencies().create(neoforgeUniversal.getDescriptor());
            neoforgeDependency.setTransitive(false);
            Configuration configuration = project.getConfigurations().detachedConfiguration(neoforgeDependency);
            Set<File> files = configuration.resolve();
            if (files.size() != 1) throw new IllegalStateException(neoforgeUniversal + " resolved to more than one file.");
            File file = files.iterator().next();
            String specVersion;
            try (JarFile zf = new JarFile(file)) {
                Attributes neoformAttributes = zf.getManifest().getAttributes("net/neoforged/neoforge/versions/neoform/");
                if (neoformAttributes == null) throw new IllegalStateException(neoforgeUniversal + " does not contain a neoform section in its manifest.");
                specVersion = neoformAttributes.getValue("Specification-Version");
                if (specVersion == null) throw new IllegalStateException("neoform manifest section has no specification version in " + neoforgeUniversal);
            }
            this.neoforgeMinecraftVersions.put(neoforge, specVersion);
            this.save();
            return specVersion;
        } catch (Exception e) {
            throw new RuntimeException("Can't infer minecraft version", e);
        }
    }

    public int resource(String minecraft) {
        return this.packVersions(minecraft).resource();
    }

    public int data(String minecraft) {
        PackVersions pack = this.packVersions(minecraft);
        return pack.data().orElse(pack.resource());
    }

    private synchronized PackVersions packVersions(String minecraft) {
        if (this.packVersions.containsKey(minecraft)) return this.packVersions.get(minecraft);
        try {
            PackVersions packVersions = null;
            try (InputStream in = this.launcher.version(minecraft).client(); ZipInputStream zin = new ZipInputStream(in)) {
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    String path = entry.getName();
                    while (path.startsWith("/")) path = path.substring(1);
                    if (path.equals("version.json")) {
                        Reader reader = new InputStreamReader(zin, StandardCharsets.UTF_8);
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        if (json.has("pack_version") && json.get("pack_version").isJsonObject() && json.get("pack_version") instanceof JsonObject packVersionObj && packVersionObj.has("resource")) {
                            packVersions = new PackVersions(packVersionObj.get("resource").getAsInt(), packVersionObj.has("data") ? Optional.of(packVersionObj.get("data").getAsInt()) : Optional.empty());
                        }
                    }
                }
            }
            if (packVersions == null) {
                throw new IOException("No valid version.json file found in client.");
            }
            this.packVersions.put(minecraft, packVersions);
            this.save();
            return packVersions;
        } catch (IOException e) {
            throw new RuntimeException("Could not get pack versions for " + minecraft, e);
        }
    }

    public synchronized int java(String minecraft) {
        if (this.javaVersions.containsKey(minecraft)) return this.javaVersions.get(minecraft);
        int javaVersion = this.launcher.version(minecraft).java();
        this.javaVersions.put(minecraft, javaVersion);
        this.save();
        return javaVersion;
    }

    private synchronized void clear() {
        this.neoforgeMinecraftVersions.clear();
        this.packVersions.clear();
        this.javaVersions.clear();
    }

    private synchronized void load() {
        try {
            this.clear();
            if (!Files.isRegularFile(this.path)) return;
            JsonObject json;
            try (Reader reader = Files.newBufferedReader(this.path)) {
                json = GSON.fromJson(reader, JsonObject.class);
            }
            if (json.has("neoforge_minecraft") && json.get("neoforge_minecraft").isJsonObject() && json.get("neoforge_minecraft") instanceof JsonObject neoforgeMinecraft) {
                for (Map.Entry<String, JsonElement> entry : neoforgeMinecraft.entrySet()) {
                   this.neoforgeMinecraftVersions.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            if (json.has("pack") && json.get("pack").isJsonObject() && json.get("pack") instanceof JsonObject pack) {
                for (Map.Entry<String, JsonElement> entry : pack.entrySet()) {
                    if (entry.getValue().isJsonArray() && entry.getValue() instanceof JsonArray array && array.size() == 2 && !array.get(0).isJsonNull()) {
                        this.packVersions.put(entry.getKey(), new PackVersions(array.get(0).getAsInt(), array.get(1).isJsonNull() ? Optional.empty() : Optional.of(array.get(1).getAsInt())));
                    }
                }
            }
            if (json.has("java") && json.get("java").isJsonObject() && json.get("java") instanceof JsonObject java) {
                for (Map.Entry<String, JsonElement> entry : java.entrySet()) {
                    this.javaVersions.put(entry.getKey(), entry.getValue().getAsInt());
                }
            }
        } catch (IOException | ClassCastException | NumberFormatException | NullPointerException e) {
            this.clear();
        }
    }

    private synchronized void save() {
        try {
            JsonObject json = new JsonObject();
            JsonObject neoforgeMinecraft = new JsonObject();
            for (Map.Entry<String, String> entry : this.neoforgeMinecraftVersions.entrySet()) {
                neoforgeMinecraft.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("neoforge_minecraft", neoforgeMinecraft);
            JsonObject pack = new JsonObject();
            for (Map.Entry<String, PackVersions> entry : this.packVersions.entrySet()) {
                JsonArray array = new JsonArray();
                array.add(entry.getValue().resource());
                array.add(entry.getValue().data().<JsonElement>map(JsonPrimitive::new).orElse(JsonNull.INSTANCE));
                pack.add(entry.getKey(), array);
            }
            json.add("pack", pack);
            JsonObject java = new JsonObject();
            for (Map.Entry<String, Integer> entry : this.javaVersions.entrySet()) {
                java.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("java", java);

            PathUtils.createParentDirectories(this.path);
            try (Writer writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                GSON.toJson(json, writer);
                writer.write("\n");
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(this.path);
            } catch (IOException x) {
                e.addSuppressed(x);
            }
        }
    }

    private record PackVersions(int resource, Optional<Integer> data) {}
}
