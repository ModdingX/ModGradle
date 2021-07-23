package io.github.noeppi_noeppi.tools.modgradle.mappings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record Names(
        Map<String, String> classes,
        Map<String, String> fields,
        Map<String, String> methods,
        Map<String, Map<Integer, String>> params
) {
    
    public static final Names EMPTY = new Names(Map.of(), Map.of(), Map.of(), Map.of());

    public Names merge(Names other) {
        Map<String, String> classes = new HashMap<>(this.classes);
        Map<String, String> fields = new HashMap<>(this.fields);
        Map<String, String> methods = new HashMap<>(this.methods);
        Map<String, Map<Integer, String>> params = new HashMap<>(this.params.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), new HashMap<>(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        classes.putAll(other.classes);
        fields.putAll(other.fields);
        methods.putAll(other.methods);
        for (Map.Entry<String, Map<Integer, String>> entry : other.params.entrySet()) {
            Map<Integer, String> map = params.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
            map.putAll(entry.getValue());
        }
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
