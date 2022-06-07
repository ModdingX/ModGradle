package org.moddingx.modgradle.mappings;

import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MappingMerger {

    // Merges by original.
    // To merge by mapped, reverse before merging and then reverse the merged mappings again.
    // Elements later in the list have a higher priority
    public static IMappingFile mergeMappings(List<IMappingFile> mappings, boolean noParams) {
        if (mappings.isEmpty()) {
            return IMappingBuilder.create("from", "to").build().getMap("from", "to");
        } else if (mappings.size() == 1) {
            return mappings.get(0);
        } else {
            IMappingBuilder builder = IMappingBuilder.create("from", "to");
            
            for (String pkg : collect(mappings, IMappingFile::getPackages)) {
                MergedMapping<IMappingFile.IPackage> merged = merge(mappings, mf -> mf.getPackage(pkg));
                if (merged != null) {
                    IMappingBuilder.IPackage pkgBuilder = builder.addPackage(pkg, merged.mappedName());
                    mergeMeta(merged.results(), pkgBuilder::meta);
                }
            }
            
            for (String cls : collect(mappings, IMappingFile::getClasses)) {
                MergedMapping<IMappingFile.IClass> merged = merge(mappings, mf -> mf.getClass(cls));
                if (merged != null) {
                    IMappingBuilder.IClass clsBuilder = builder.addClass(cls, merged.mappedName());
                    mergeClass(clsBuilder, merged.results, noParams);
                    mergeMeta(merged.results(), clsBuilder::meta);
                }
            }
            
            return builder.build().getMap("from", "to");
        }
    }

    private static void mergeClass(IMappingBuilder.IClass builder, List<IMappingFile.IClass> classes, boolean noParams) {
        for (String fd : collect(classes, IMappingFile.IClass::getFields)) {
            MergedMapping<IMappingFile.IField> merged = merge(classes, cls -> cls.getField(fd));
            if (merged != null) {
                IMappingBuilder.IField fieldBuilder = builder.field(fd, merged.mappedName());
                // Take last element of stream as last mapping file has the highest priority
                Optional<String> fieldDesc = merged.results().stream().flatMap(node -> Stream.ofNullable(node.getDescriptor())).reduce((first, second) -> second);
                fieldDesc.ifPresent(fieldBuilder::descriptor);
                mergeMeta(merged.results(), fieldBuilder::meta);
            }
        }
        
        for (MethodKey md : collect(classes, IMappingFile.IClass::getMethods, node -> new MethodKey(node.getOriginal(), node.getDescriptor()))) {
            MergedMapping<IMappingFile.IMethod> merged = merge(classes, cls -> cls.getMethod(md.name(), md.desc()));
            if (merged != null) {
                IMappingBuilder.IMethod methodBuilder = builder.method(md.desc(), md.name(), merged.mappedName());
                mergeMethod(methodBuilder, merged.results(), noParams);
                mergeMeta(merged.results(), methodBuilder::meta);
            }
        }
    }

    private static void mergeMethod(IMappingBuilder.IMethod builder, List<IMappingFile.IMethod> methods, boolean noParams) {
        if (!noParams) {
            Map<Integer, List<IMappingFile.IParameter>> paramMap = new HashMap<>();
            Map<Integer, String> original = new HashMap<>();
            Map<Integer, String> mapped = new HashMap<>();
            for (IMappingFile.IMethod md : methods) {
                for (IMappingFile.IParameter param : md.getParameters()) {
                    paramMap.computeIfAbsent(param.getIndex(), k -> new ArrayList<>()).add(param);
                    original.put(param.getIndex(), param.getOriginal());
                    mapped.put(param.getIndex(), param.getMapped());
                }
            }
            for (int idx : paramMap.keySet().stream().sorted().toList()) {
                if (original.containsKey(idx) && mapped.containsKey(idx)) {
                    IMappingBuilder.IParameter paramBuilder = builder.parameter(idx, original.get(idx), mapped.get(idx));
                    mergeMeta(paramMap.get(idx), paramBuilder::meta);
                }
            }
        }
    }
    
    private static <M, X extends IMappingFile.INode> List<String> collect(List<M> mappings, Function<M, ? extends Collection<? extends X>> extractor) {
        return collect(mappings, extractor, IMappingFile.INode::getOriginal);
    }
    
    private static <M, X, T> List<T> collect(List<M> mappings, Function<M, ? extends Collection<? extends X>> extractor, Function<X, T> keys) {
        Set<T> results = mappings.stream()
                .flatMap(m -> Stream.ofNullable(extractor.apply(m)))
                .flatMap(Collection::stream)
                .map(keys)
                .collect(Collectors.toSet());
        return results.stream().sorted().toList();
    }
    
    @Nullable
    private static <M, T extends IMappingFile.INode> MergedMapping<T> merge(List<M> mappings, Function<M, T> extractor) {
        List<T> results = mappings.stream().flatMap(m -> Stream.ofNullable(extractor.apply(m))).toList();
        if (results.isEmpty()) return null;
        String mappedName = results.get(results.size() - 1).getMapped();
        return new MergedMapping<>(results, mappedName);
    }

    private static void mergeMeta(List<? extends IMappingFile.INode> nodes, BiConsumer<String, String> meta) {
        Map<String, String> metaMap = new HashMap<>();
        // Collect all metadata from all nodes. Later nodes replace the ones before
        for (IMappingFile.INode node : nodes) {
            metaMap.putAll(node.getMetadata());
        }
        // Apply all the collected metadata
        for (Map.Entry<String, String> entry : metaMap.entrySet()) {
            meta.accept(entry.getKey(), entry.getValue());
        }
    }

    private record MethodKey(String name, String desc) implements Comparable<MethodKey> {

        @Override
        public int compareTo(MethodKey o) {
            int res = this.name.compareTo(o.name);
            return res != 0 ? res : this.desc.compareTo(o.desc);
        }
    }
    
    private record MergedMapping<T>(List<T> results, String mappedName) {}
}
