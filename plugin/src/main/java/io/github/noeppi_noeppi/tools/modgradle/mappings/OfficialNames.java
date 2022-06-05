package io.github.noeppi_noeppi.tools.modgradle.mappings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;

import javax.annotation.WillClose;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class OfficialNames {
    
    public static IMappingFile readOfficialMappings(String mcv) throws IOException {
        JsonElement manifest = readJson(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"));
        for (JsonElement elem : manifest.getAsJsonObject().get("versions").getAsJsonArray()) {
            JsonObject entry = elem.getAsJsonObject();
            if (mcv.equals(entry.get("id").getAsString())) {
                JsonElement versionJson = readJson(new URL(entry.get("url").getAsString()));
                URL clientMappings = new URL(versionJson.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("client_mappings").get("url").getAsString());
                URL serverMappings = new URL(versionJson.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("server_mappings").get("url").getAsString());
                return readOfficialMappings(clientMappings.openStream(), serverMappings.openStream());
            }
        }
        throw new IllegalStateException("Minecraft version not found in launchermeta: " + mcv);
    }
    
    private static JsonElement readJson(URL url) throws IOException {
        Reader in = new InputStreamReader(url.openStream());
        JsonElement json = ModGradle.INTERNAL.fromJson(in, JsonElement.class);
        in.close();
        return json;
    }
    
    private static IMappingFile readOfficialMappings(@WillClose InputStream client, @WillClose InputStream server) throws IOException {
        IMappingFile clientMap = IMappingFile.load(client).reverse();
        IMappingFile serverMap = IMappingFile.load(server).reverse();
        return mergeNoParam(clientMap, serverMap);
    }
    
    private static IMappingFile mergeNoParam(IMappingFile m1, IMappingFile m2) {
        IMappingBuilder builder = IMappingBuilder.create("obf", "named");
        m1.getClasses().forEach(cls -> {
            IMappingBuilder.IClass newClass = builder.addClass(cls.getOriginal(), cls.getMapped());
            
            cls.getFields().forEach(f -> newClass.field(f.getOriginal(), f.getMapped()));
            cls.getMethods().forEach(m -> newClass.method(m.getDescriptor(), m.getOriginal(), m.getMapped()));
        });
        m2.getClasses().forEach(cls -> {
            IMappingBuilder.IClass newClass = builder.addClass(cls.getOriginal(), cls.getMapped());
            
            cls.getFields().forEach(f -> newClass.field(f.getOriginal(), f.getMapped()));
            cls.getMethods().forEach(m -> newClass.method(m.getDescriptor(), m.getOriginal(), m.getMapped()));
        });
        return builder.build().getMap("obf", "named");
    }
}
