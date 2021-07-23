package io.github.noeppi_noeppi.tools.modgradle.mappings;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Remapper from old srg to new srg
public class SrgRemapper {

    public static final Pattern OLD_SRG_F = Pattern.compile("^field_\\d+_\\w+$");
    public static final Pattern OLD_SRG_M = Pattern.compile("^func_(\\d+)_\\w+$");
    public static final Pattern OLD_SRG_P = Pattern.compile("^p_(\\d+)_(\\d+)_$");
    public static final Pattern OLD_SRG_PI = Pattern.compile("^p_i(\\d+)_(\\d+)_$");
    private static final Pattern OLD_CTOR = Pattern.compile("^(\\d+)\\s+(\\S+)\\s+(\\S+)+$");

    private final Map<String, String> srgMap;
    private final BiMap<String, String> classes;
    private final ParamRemapper params;
    private final Map<Integer, IMappingFile.IMethod> srgFuncMap;
    private final Map<Integer, IMappingFile.IMethod> srgCtorIdMap;
    private final Set<Integer> oldStatics;

    private SrgRemapper(ImmutableMap<String, String> srgMap, ImmutableBiMap<String, String> classes, ParamRemapper params, Map<Integer, IMappingFile.IMethod> srgFuncMap, Map<Integer, IMappingFile.IMethod> srgCtorIdMap, Set<Integer> oldStatics) {
        this.srgMap = srgMap;
        this.classes = classes;
        this.params = params;
        this.srgFuncMap = srgFuncMap;
        this.srgCtorIdMap = srgCtorIdMap;
        this.oldStatics = oldStatics;
    }

    public Map<String, String> getClassMap() {
        return this.classes;
    }

    public Optional<String> remapSrg(String oldSRG) {
        return Optional.ofNullable(this.srgMap.getOrDefault(oldSRG, null));
    }

    public Optional<String> getClassName(String newSRG) {
        return Optional.ofNullable(this.classes.getOrDefault(newSRG, null));
    }

    public Optional<String> reverseClassName(String newSRG) {
        return Optional.ofNullable(this.classes.inverse().getOrDefault(newSRG, null));
    }

    public Optional<String> getMethodSignature(String signature) {
        return MappingHelper.transformMethodSignature(signature, this.classes);
    }

    public Optional<String> reverseMethodSignature(String signature) {
        return MappingHelper.transformMethodSignature(signature, this.classes.inverse());
    }

    public BaseNames remapNames(BaseNames names) {
        // Should not happen but sometimes a parameter is mapped twice. (Seems like theres an issue with
        // converting the old parameter format to a method and a an index)
        // That means names may be inconsistent but that does not really matter as it does not break
        // anything. But we need to do it like this here because ImmutableMap#toImmutableMap would fail.
        Map<String, String> params = new HashMap<>();
        names.params().entrySet().stream()
                .flatMap(e ->
                        this.remapParam(e.getKey()).stream()
                                .map(v -> Pair.of(v, e.getValue()))
                )
                .forEach(e -> params.put(e.getKey(), e.getValue()));
        return new BaseNames(
                names.fields().entrySet().stream()
                        .map(e -> Pair.of(this.remapSrg(e.getKey()).orElse(e.getKey()), e.getValue()))
                        .collect(ImmutableMap.toImmutableMap(Pair::getKey, Pair::getValue)),
                names.methods().entrySet().stream()
                        .map(e -> Pair.of(this.remapSrg(e.getKey()).orElse(e.getKey()), e.getValue()))
                        .collect(ImmutableMap.toImmutableMap(Pair::getKey, Pair::getValue)),
                ImmutableMap.copyOf(params)
        );
    }

    public Set<String> remapParam(String param) {
        Matcher match = OLD_SRG_P.matcher(param);
        if (match.matches()) {
            int mid = Integer.parseInt(match.group(1));
            int oldIdx = Integer.parseInt(match.group(2));
            IMappingFile.IMethod method = this.srgFuncMap.get(mid);
            if (method == null) return Collections.emptySet();
            int idx = this.getNewIndex(method, mid, oldIdx, false);
            return this.params.getParamNames(method.getMapped(), idx);
        }
        match = OLD_SRG_PI.matcher(param);
        if (match.matches()) {
            int mid = Integer.parseInt(match.group(1));
            int oldIdx = Integer.parseInt(match.group(2));
            IMappingFile.IMethod method = this.srgCtorIdMap.get(mid);
            if (method == null) return Collections.emptySet();
            int idx = this.getNewIndex(method, mid, oldIdx, true);
            return this.params.getParamNames("i_" + method.getMapped() + "_", idx);
        }
        return Collections.emptySet();
    }

    // Uses real index not old mcp index
    public Optional<Set<String>> remapParam(String method, int idx) {
        return this.remapSrg(method).map(n -> this.params.getParamNames(n, idx));
    }

    private int getNewIndex(IMappingFile.IMethod method, int methodId, int oldIdx, boolean ctor) {
        int off = ctor || this.oldStatics.contains(methodId) ? 0 : 1;
        int idx = 0;
        String args = this.getReducedArgStr(method.getDescriptor());
        for (char chr : args.toCharArray()) {
            if (off >= oldIdx) break;
            off += (chr == 'J' || chr == 'D') ? 2 : 1;
            idx += 1;
        }
        return idx;
    }

    private String getReducedArgStr(String desc) {
        desc = desc.strip();
        if (desc.startsWith("(")) desc = desc.substring(1).strip();
        StringBuilder sb = new StringBuilder();
        boolean skipping = false;
        for (char chr : desc.toCharArray()) {
            if (skipping) {
                if (chr == ';') skipping = false;
                continue;
            }
            if (chr == ')') return sb.toString();
            if (chr == 'L') skipping = true;
            sb.append(chr);
        }
        return sb.toString();
    }

    public static SrgRemapper create(@WillClose InputStream source, @WillClose InputStream target, @Nullable Map<String, String> classRemap, boolean verbose) throws IOException {
        MappingExtractor.SrgInfo from = MappingExtractor.extractSrg(source);
        INamedMappingFile to = MappingExtractor.extractSrg2(target);
        IMappingFile fromSrg = from.srg();
        IMappingFile toSrg = to.getMap("obf", "srg");
        IMappingFile toId = to.getMap("obf", "id");
        Map<String, String> srgMap = new HashMap<>();
        BiMap<String, String> classes = HashBiMap.create();
        Map<Integer, IMappingFile.IMethod> srgFuncMap = new HashMap<>();
        Map<Integer, IMappingFile.IMethod> srgCtorIdMap = new HashMap<>();
        addSrgAndClassMap(fromSrg, toSrg, classes, srgMap, srgFuncMap, classRemap, verbose);
        IMappingFile reverseFrom = fromSrg.reverse();
        for (String ctorLine : from.ctors()) {
            addCtorMapLine(ctorLine, reverseFrom, toId, srgCtorIdMap, verbose);
        }
        ParamRemapper params = ParamRemapper.create(to);
        return new SrgRemapper(ImmutableMap.copyOf(srgMap), ImmutableBiMap.copyOf(classes), params, ImmutableMap.copyOf(srgFuncMap), ImmutableMap.copyOf(srgCtorIdMap), from.statics());
    }

    private static void addSrgAndClassMap(IMappingFile from, IMappingFile to, BiMap<String, String> classes, Map<String, String> srgMap, Map<Integer, IMappingFile.IMethod> srgFuncMap, @Nullable Map<String, String> classRemap, boolean verbose) {
        from.getClasses().forEach(cls -> {
            IMappingFile.IClass targetClass = to.getClass(cls.getOriginal());
            if (targetClass == null) {
                if (verbose)
                    System.out.println("Building SRG remapper: Target mappings are missing a class from source mappings: " + cls.getOriginal() + " (" + cls.getMapped() + ")");
            } else if (classes.containsKey(targetClass.getMapped()) || classes.containsValue(cls.getMapped())) {
                throw new IllegalStateException("Class mapped twice: " + cls.getOriginal() + " (" + cls.getMapped() + " -> " + targetClass.getMapped() + ")");
            } else {
                if (classRemap != null) {
                    classes.put(classRemap.getOrDefault(cls.getOriginal(), targetClass.getMapped()), cls.getMapped());
                } else {
                    classes.put(targetClass.getMapped(), cls.getMapped());
                }
                cls.getFields().stream()
                        .filter(f -> OLD_SRG_F.matcher(f.getMapped()).matches())
                        .forEach(f -> {
                            IMappingFile.IField targetField = targetClass.getField(f.getOriginal());
                            if (targetField != null) {
                                if (srgMap.containsKey(f.getMapped())) {
                                    throw new IllegalStateException("Field mapped twice: " + cls.getOriginal() + " " + f.getOriginal() + " (" + cls.getMapped() + " " + f.getMapped() + " -> " + targetClass.getMapped() + " " + targetField.getMapped() + ")");
                                }
                                srgMap.put(f.getMapped(), targetField.getMapped());
                            } else {
                                if (verbose)
                                    System.out.println("Building SRG remapper: Target mappings are missing a field from source mappings: " + cls.getOriginal() + " " + f.getOriginal() + " (" + cls.getMapped() + " " + f.getMapped() + ")");
                            }
                        });
                cls.getMethods().stream()
                        .filter(m -> OLD_SRG_M.matcher(m.getMapped()).matches())
                        .forEach(m -> {
                            IMappingFile.IMethod targetMethod = targetClass.getMethod(m.getOriginal(), m.getDescriptor());
                            if (targetMethod != null && !"<clinit>".equals(targetMethod.getMapped()) && !"<init>".equals(targetMethod.getMapped())) {
                                // Don't throw, methods may override each other.
                                if (!srgMap.containsKey(m.getMapped())) {
                                    srgMap.put(m.getMapped(), targetMethod.getMapped());
                                    Matcher match = OLD_SRG_M.matcher(m.getMapped());
                                    if (match.matches()) {
                                        srgFuncMap.put(Integer.parseInt(match.group(1)), targetMethod);
                                    }
                                }
                            } else {
                                if (verbose)
                                    System.out.println("Building SRG remapper: Target mappings are missing a method from source mappings: " + cls.getOriginal() + " " + m.getOriginal() + m.getDescriptor() + " (" + cls.getMapped() + " " + m.getMapped() + m.getMappedDescriptor() + ")");
                            }
                        });
            }
        });
    }

    private static void addCtorMapLine(String line, IMappingFile reverseFrom, IMappingFile to, Map<Integer, IMappingFile.IMethod> srgCtorMap, boolean verbose) {
        line = line.strip();
        if (!line.isEmpty()) {
            Matcher match = OLD_CTOR.matcher(line);
            if (match.matches()) {
                int id = Integer.parseInt(match.group(1));
                String cls = match.group(2);
                String oldSig = match.group(3);
                IMappingFile.IClass oldCls = reverseFrom.getClass(cls);
                if (oldCls == null) {
                    if (verbose)
                        System.out.println("Building SRG remapper: Class for a constructor in constructors.txt is not found in source mappings: " + cls + " " + oldSig + " (" + id + ")");
                } else {
                    IMappingFile.IClass newCls = to.getClass(oldCls.getMapped());
                    String newSig = reverseFrom.remapDescriptor(oldSig);
                    IMappingFile.IMethod method = newCls == null ? null : newCls.getMethod("<init>", newSig);
                    if (method == null) {
                        if (verbose)
                            System.out.println("Building SRG remapper: Target mappings are missing a constructor from source mappings: " + oldCls.getOriginal() + " <init>" + oldSig + " (" + oldCls.getMapped() + "@" + newSig + ") Cause: " + (newCls == null ? "Class not found" : "Missing <init> method."));
                    } else if (srgCtorMap.containsKey(id)) {
                        throw new IllegalStateException("Constructor mapped twice: " + oldCls.getOriginal() + " " + id + " -> " + newCls.getMapped() + " " + method.getMapped() + method.getMappedDescriptor() + ")");
                    } else {
                        srgCtorMap.put(id, method);
                    }
                }
            } else {
                throw new IllegalStateException("Invalid line in constructors.txt: '" + line + "'");
            }
        }
    }
}
