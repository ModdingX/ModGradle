package io.github.noeppi_noeppi.tools.modgradle.plugins.mcupdate;

import io.github.noeppi_noeppi.tools.modgradle.mappings.*;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IRenamer;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.InputChanges;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuildReMappingsTask extends DefaultTask {

    private final Property<String> mappings = this.getProject().getObjects().property(String.class);
    private final Property<URL> source = this.getProject().getObjects().property(URL.class);
    private final RegularFileProperty output = this.getProject().getObjects().fileProperty();
    private final Property<URL> official_client = this.getProject().getObjects().property(URL.class);
    private final Property<URL> official_server = this.getProject().getObjects().property(URL.class);
    private final Property<URL> parchment = this.getProject().getObjects().property(URL.class);
    private final ListProperty<URL> additional_srgs = this.getProject().getObjects().listProperty(URL.class);
    
    public BuildReMappingsTask() {
        this.mappings.convention(new DefaultProvider<>(() -> null));
        this.output.convention(new DefaultProvider<>(() -> (RegularFile) () -> this.getProject().file("build").toPath().resolve(this.getName()).resolve("base.tsrg").toFile()));
        try {
            this.source.convention(new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.16.5-20210115.111550/mcp_config-1.16.5-20210115.111550.zip"));
            this.official_client.convention(new URL("https://launcher.mojang.com/v1/objects/374c6b789574afbdc901371207155661e0509e17/client.txt"));
            this.official_server.convention(new URL("https://launcher.mojang.com/v1/objects/41285beda6d251d190f2bf33beadd4fee187df7a/server.txt"));
            this.parchment.convention(new URL("https://maven.parchmentmc.org/org/parchmentmc/data/parchment-1.17/2021.07.21/parchment-1.17-2021.07.21.zip"));
            this.additional_srgs.convention(Collections.singleton(new URL("https://raw.githubusercontent.com/noeppi-noeppi/LibX/1.17/mcupdate.csrg")));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to configure conventions for BuildReMappingsTask", e);
        }
    }

    @Input
    @Nullable
    public String getMappings() {
        return this.mappings.get();
    }

    public void setMappings(String mappings) {
        this.mappings.set(mappings);
    }

    @Input
    public URL getSource() {
        return this.source.get();
    }

    public void setSource(URL source) {
        this.source.set(source);
    }

    @Input
    public URL getOfficialClient() {
        return this.official_client.get();
    }

    public void setOfficialClient(URL officialClient) {
        this.official_client.set(officialClient);
    }
    
    @Input
    public URL getOfficialServer() {
        return this.official_server.get();
    }

    public void setOfficialServer(URL officialServer) {
        this.official_server.set(officialServer);
    }

    @Input
    @Nullable
    public URL getParchment() {
        return this.parchment.get();
    }

    public void setParchment(@Nullable URL parchment) {
        this.parchment.set(parchment);
    }
    
    @OutputFile
    public RegularFile getOutput() {
        return this.output.get();
    }

    public void setOutput(RegularFile output) {
        this.output.set(output);
    }
    
    @Input
    public List<URL> getAdditionalSrgs() {
        return this.additional_srgs.get();
    }
    
    public void setAdditionalSrgs(Iterable<URL> additionalSrgs) {
        this.additional_srgs.set(additionalSrgs);
    }
    
    public void additionalSrg(URL additionalSrg) {
        this.addAdditionalSrg(additionalSrg);
    }
    
    public void additionalSrg(String additionalSrg) throws MalformedURLException {
        this.additionalSrg(new URL(additionalSrg));
    }
    
    public void addAdditionalSrg(URL additionalSrg) {
        this.additional_srgs.add(additionalSrg);
    }
    
    @TaskAction
    protected void remapSources(InputChanges inputs) throws IOException {
        String mappings = this.getMappings();
        if (mappings == null) throw new IllegalStateException("Can't remap sources: Mappings not set.");
        String channel = mappings.substring(0, mappings.indexOf('_'));
        String version = mappings.substring(mappings.indexOf('_') + 1);
        Names names = switch (channel) {
            case "stable", "snapshot" -> OldMappingReader.readOldMappings(new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_" + channel + "/" + version + "/mcp_" + channel + "-" + version + ".zip").openStream(), false, false);
            case "official", "parchment" -> {
                System.out.println("Running mcupdate on official names is discouraged.");
                yield Names.EMPTY;
            }
            // ModUtils channels
            case "unofficial" -> OldMappingReader.readOldMappings(new URL("https://noeppi-noeppi.github.io/MappingUtilities/mcp_unofficial/" + version + ".zip").openStream(), false, true);
            case "custom" -> throw new IllegalStateException("Remapping sources from custom names is not supported.");
            default -> throw new IllegalStateException("Can't remap sources: Invalid mappings: " + mappings);
        };
        MappingIO.SrgInfo from = MappingIO.readMcpConfigSrg(this.getSource().openStream());
        IMappingFile fromSrg = from.srg();
        IMappingFile official = OfficialNames.readOfficialMappings(this.getOfficialClient().openStream(), this.getOfficialServer().openStream());
        
        URL parchment = this.getParchment();
        if (parchment != null) {
            official = ParchmentLoader.loadParchment(official, parchment.openStream(), fromSrg, from.statics());
        }
        
        IMappingFile fromSrgNamed = fromSrg.rename(new IRenamer() {
            @Override
            public String rename(IMappingFile.IField value) {
                return names.fields().getOrDefault(value.getMapped(), value.getMapped());
            }

            @Override
            public String rename(IMappingFile.IMethod value) {
                return names.methods().getOrDefault(value.getMapped(), value.getMapped());
            }
        });
        
        // We can't chain normally as this would remove params. So we reverse, then chain
        // and then reverse again.
        IMappingFile target = official.reverse().chain(fromSrgNamed).reverse();
        
        List<URL> additionalSrgs = this.getAdditionalSrgs();
        if (!additionalSrgs.isEmpty()) {
            List<IMappingFile> additionalMappings = new ArrayList<>();
            for (URL url : additionalSrgs) {
                try {
                    additionalMappings.add(IMappingFile.load(url.openStream()));
                } catch (FileNotFoundException e) {
                    System.out.println("Failed to load additional SRG: " + url);
                }
            }
            target = MappingMerger.mergeMappings(target, additionalMappings, false);
        }

        Files.createDirectories(this.getOutput().getAsFile().toPath().getParent());
        // tsrg2 because we need params
        target.write(this.getOutput().getAsFile().toPath(), IMappingFile.Format.TSRG2, false);
    }
}
