package io.github.noeppi_noeppi.tools.modgradle.mappings;

import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MappingFileNameLoader {

    public static Names loadFromMappings(INamedMappingFile srg, IMappingFile obfNamed) {
        Map<String, String> classes = new HashMap<>();
        Map<String, String> fields = new HashMap<>();
        Map<String, String> methods = new HashMap<>();
        Map<String, Map<Integer, String>> params = new HashMap<>();
        
        IMappingFile srgMap = srg.getMap("obf", "srg");
        IMappingFile idMap = srg.getMap("obf", "id");
        
        obfNamed.getClasses().forEach(cls -> {
            IMappingFile.IClass targetClass = srgMap.getClass(cls.getOriginal());
            if (targetClass != null) {
                classes.put(MappingHelper.getSimplifiedClassSrg(targetClass.getMapped()), cls.getMapped());
                cls.getFields().forEach(f -> {
                    IMappingFile.IField targetField = targetClass.getField(f.getOriginal());
                    if (targetField != null) {
                        fields.put(targetField.getMapped(), f.getMapped());
                    }
                });
                cls.getMethods().forEach(m -> {
                    IMappingFile.IMethod targetMethod = targetClass.getMethod(m.getOriginal(), m.getDescriptor());
                    if (targetMethod != null && !"<clinit>".equals(targetMethod.getMapped())) {
                        if ("<init>".equals(targetMethod.getMapped())) {
                            IMappingFile.IClass idClass = idMap.getClass(cls.getOriginal());
                            if (idClass != null) {
                                IMappingFile.IMethod idMethod = idClass.getMethod(m.getOriginal(), m.getDescriptor());
                                if (idMethod != null) {
                                    String baseName = "i_" + idMethod.getMapped() + "_";
                                    m.getParameters().forEach(p -> {
                                        Map<Integer, String> map = params.computeIfAbsent(baseName, k -> new HashMap<>());
                                        map.put(p.getIndex(), p.getMapped());
                                    });
                                }
                            }
                        } else {
                            methods.put(targetMethod.getMapped(), m.getMapped());
                            m.getParameters().forEach(p -> {
                                Map<Integer, String> map = params.computeIfAbsent(targetMethod.getMapped(), k -> new HashMap<>());
                                map.put(p.getIndex(), p.getMapped());
                            });
                        }
                    }
                });
            }
        });
        
        return new Names(
                Collections.unmodifiableMap(classes),
                Collections.unmodifiableMap(fields),
                Collections.unmodifiableMap(methods),
                Collections.unmodifiableMap(params.entrySet().stream()
                        .map(e -> Map.entry(e.getKey(), Collections.unmodifiableMap(e.getValue())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                )
        );
    }
}
