package io.github.noeppi_noeppi.tools.modgradle.mappings;

import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class MappingMerger {

    // Merges by original.
    // To merge by mapped, reverse before merging and then reverse the merged mappings again.
    public static IMappingFile mergeMappings(@Nullable IMappingFile main, List<IMappingFile> others, boolean noparam) {
        if (main != null && others.isEmpty() && !noparam) {
            return main;
        }
        List<IMappingFile> mappings = new ArrayList<>();
        if (main != null) mappings.add(main);
        mappings.addAll(others);
        IMappingBuilder builder = IMappingBuilder.create("from", "to");
        for (IMappingFile mf : mappings) {
            for (IMappingFile.IPackage pkg : mf.getPackages()) {
                builder.addPackage(pkg.getOriginal(), pkg.getMapped());
            }
        }
        Set<String> classes = new HashSet<>();
        for (IMappingFile mf : mappings) {
            classes.addAll(mf.getClasses().stream().map(IMappingFile.IClass::getOriginal).collect(Collectors.toSet()));
        }
        for (String cls : classes) {
            addClassValues(builder, cls, mappings, noparam);
        }
        return builder.build().getMap("from", "to");
    }

    private static void addClassValues(IMappingBuilder builder, String name, List<IMappingFile> mappings, boolean noparam) {
        IMappingBuilder.IClass clsResult = null;
        for (IMappingFile mf : mappings) {
            IMappingFile.IClass result = mf.getClass(name);
            if (result != null) {
                clsResult = builder.addClass(name, result.getMapped());
                break;
            }
        }
        IMappingBuilder.IClass cls = clsResult;
        if (cls != null) {
            Set<String> fields = new HashSet<>();
            Set<String> methods = new HashSet<>();
            for (IMappingFile mf : mappings) {
                IMappingFile.IClass resultClass = mf.getClass(name);
                if (resultClass != null) {
                    resultClass.getFields().forEach(field -> {
                        if (!fields.contains(field.getOriginal())) {
                            fields.add(field.getOriginal());
                            IMappingBuilder.IField fieldBuilder = cls.field(field.getOriginal(), field.getMapped());
                            if (field.getDescriptor() != null) fieldBuilder.descriptor(field.getDescriptor());
                        }
                    });
                    resultClass.getMethods().forEach(method -> {
                        String methodId = method.getOriginal() + method.getDescriptor();
                        if (!methods.contains(methodId)) {
                            methods.add(methodId);
                            IMappingBuilder.IMethod methodBuilder = cls.method(method.getDescriptor(), method.getOriginal(), method.getMapped());
                            if (!noparam) {
                                for (IMappingFile.IParameter param : method.getParameters()) {
                                    methodBuilder.parameter(param.getIndex(), param.getOriginal(), param.getMapped());
                                }
                            }
                        }
                    });
                }
            }
        }
    }
}
