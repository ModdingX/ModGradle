package io.github.noeppi_noeppi.tools.modgradle.mappings;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("ClassCanBeRecord")
public class ParamRemapper {

    private final Map<String, Map<Integer, Set<String>>> paramNames;

    private ParamRemapper(Map<String, Map<Integer, Set<String>>> paramNames) {
        this.paramNames = paramNames.entrySet().stream()
                .<Pair<String, Map<Integer, Set<String>>>>map(e -> Pair.of(
                        e.getKey(),
                        e.getValue().entrySet().stream()
                                .map(f -> Pair.of(f.getKey(), ImmutableSet.copyOf(f.getValue())))
                                .collect(ImmutableMap.toImmutableMap(Pair::getKey, Pair::getValue))
                ))
                .collect(ImmutableMap.toImmutableMap(Pair::getKey, Pair::getValue));
    }
    
    public Set<String> getParamNames(String func, int idx) {
        if (this.paramNames.containsKey(func)) {
            return this.paramNames.get(func).getOrDefault(idx, Set.of());
        } else {
            return Set.of();
        }
    }
    
    public static ParamRemapper create(INamedMappingFile nmf) {
        IMappingFile srgMap = nmf.getMap("obf", "srg");
        IMappingFile idMap = nmf.getMap("obf", "id");
        Map<String, Map<Integer, Set<String>>> paramMap = new HashMap<>();
        //noinspection CodeBlock2Expr
        srgMap.getClasses().forEach(cls -> {
            cls.getMethods().forEach(m -> {
                String mname = m.getMapped();
                if (mname.equals("<init>")) {
                    IMappingFile.IClass idClass = idMap.getClass(cls.getOriginal());
                    IMappingFile.IMethod idMethod = idClass == null ? null : idClass.getMethod(m.getOriginal(), m.getDescriptor());
                    mname = idMethod == null ? null : "i_" + idMethod.getMapped() + "_";
                }
                if (mname != null) {
                    Map<Integer, Set<String>> map = paramMap.computeIfAbsent(m.getMapped(), key -> new HashMap<>());
                    for (IMappingFile.IParameter param : m.getParameters()) {
                        Set<String> nameSet = map.computeIfAbsent(param.getIndex(), key -> new HashSet<>());
                        nameSet.add(param.getMapped());
                    }
                }
            });
        });
        return new ParamRemapper(paramMap);
    }
}
