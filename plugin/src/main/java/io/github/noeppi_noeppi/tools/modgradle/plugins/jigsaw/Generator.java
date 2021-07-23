package io.github.noeppi_noeppi.tools.modgradle.plugins.jigsaw;

import io.github.noeppi_noeppi.tools.modgradle.plugins.jigsaw.parser.*;
import io.github.noeppi_noeppi.tools.modgradle.util.JavaHelper;
import io.github.noeppi_noeppi.tools.modgradle.util.Reflector;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Generator {

    public static void generate(Path input, Path output, List<Path> sourceDirs) throws IOException {
        
        Set<String> packages = JavaHelper.findPackages(sourceDirs);

        Reader reader = Files.newBufferedReader(input);
        // We need to place java classes into the java folder as scalac won't compile with java 16
        // sources. However here we can't directly access the scala classes.
        ParsedModule module = (ParsedModule) Reflector.callScala(
                () -> Class.forName("io.github.noeppi_noeppi.tools.modgradle.plugins.jigsaw.parser.ModuleParser")
                    .getMethod("parse", Reader.class),
                reader);
        reader.close();

        Set<String> imports = new HashSet<>();
        Map<String, RequireType> requires = new HashMap<>();
        Map<String, Set<String>> exports = new HashMap<>();
        Map<String, Set<String>> opens = new HashMap<>();
        Set<String> spi = new HashSet<>();
        for (Statement stmt : module.statements()) {
            if (stmt instanceof ImportStatement s) {
                imports.add(s.str());
            } else if (stmt instanceof RequireStatement s) {
                for (String m : s.modules()) {
                    if (requires.containsKey(m)) {
                        RequireType rt = requires.get(m);
                        requires.put(m, rt.merge(new RequireType(s.isStatic(), s.isTransitive())));
                    } else {
                        requires.put(m, new RequireType(s.isStatic(), s.isTransitive()));
                    }
                }
            } else if (stmt instanceof ExportsStatement s) {
                addAll(packages.stream().filter(s.packages().getMatcher()).collect(Collectors.toSet()), s.targets(), exports);
            } else if (stmt instanceof OpensStatement s) {
                addAll(packages.stream().filter(s.packages().getMatcher()).collect(Collectors.toSet()), s.targets(), opens);
            } else if (stmt instanceof SpiStatement s) {
                spi.add(s.str());
            }
        }

        if (module.exported()) {
            addAll(packages, null, exports);
        }
        if (module.open()) {
            addAll(packages, null, opens);
        }

        Writer w = Files.newBufferedWriter(output);

        for (String i : imports) w.write(i + ";\n");
        if (!imports.isEmpty()) w.write("\n");

        if (module.open()) w.write("open ");
        w.write("module ");
        w.write(module.name());
        w.write(" {\n");

        if (!requires.isEmpty()) w.write("\n");
        for (Map.Entry<String, RequireType> require : requires.entrySet()) {
            w.write("    requires ");
            if (require.getValue().isStatic()) w.write("static ");
            if (require.getValue().isTransitive()) w.write("transitive ");
            w.write(require.getKey() + ";\n");
        }

        if (!exports.isEmpty()) w.write("\n");
        for (Map.Entry<String, Set<String>> export : exports.entrySet()) {
            w.write("    exports ");
            w.write(export.getKey());
            if (!export.getValue().contains("") && !export.getValue().isEmpty()) {
                w.write(" to ");
                w.write(export.getValue().stream().sorted().collect(Collectors.joining(", ")));
            }
            w.write(";\n");
        }

        if (!module.open()) {
            if (!opens.isEmpty()) w.write("\n");
            for (Map.Entry<String, Set<String>> open : opens.entrySet()) {
                w.write("    opens ");
                w.write(open.getKey());
                if (!open.getValue().contains("") && !open.getValue().isEmpty()) {
                    w.write(" to ");
                    w.write(open.getValue().stream().sorted().collect(Collectors.joining(", ")));
                }
                w.write(";\n");
            }
        }

        if (!spi.isEmpty()) w.write("\n");
        for (String i : spi) w.write(i + ";\n");

        w.write("}\n");
        w.close();
    }

    private static void addAll(Set<String> packages, @Nullable List<String> modules, Map<String, Set<String>> map) {
        for (String pkg : packages) {
            Set<String> set = map.computeIfAbsent(pkg, x -> new HashSet<>());
            if (!set.contains("")) {
                if (modules == null) {
                    set.clear();
                    set.add("");
                } else {
                    set.addAll(modules);
                }
            }
        }
    }

    private static record RequireType(boolean isStatic, boolean isTransitive) {

        public RequireType merge(RequireType other) {
            return new RequireType(this.isStatic && other.isStatic, this.isTransitive | other.isTransitive);
        }
    }
}