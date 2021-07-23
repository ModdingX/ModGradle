package io.github.noeppi_noeppi.tools.modgradle.mappings;

import com.google.common.collect.ImmutableMap;
import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;

import javax.annotation.WillClose;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class OfficialNames {
    
    public static Map<String, String> readOfficialClassMap(@WillClose InputStream in) throws IOException {
        IMappingFile mappings = IMappingFile.load(in).reverse();
        in.close();
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        mappings.getClasses().forEach(cls -> builder.put(cls.getOriginal(), cls.getMapped()));
        return builder.build();
    }
    
    public static Map<String, String> readOfficialClassMap(@WillClose InputStream client, @WillClose InputStream server) throws IOException {
        Map<String, String> serverMap = readOfficialClassMap(server);
        Map<String, String> clientMap = readOfficialClassMap(client);
        Map<String, String> map = new HashMap<>();
        map.putAll(serverMap);
        map.putAll(clientMap);
        return ImmutableMap.copyOf(map);
    }
    
    public static IMappingFile readOfficialMappings(@WillClose InputStream client, @WillClose InputStream server) throws IOException {
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
