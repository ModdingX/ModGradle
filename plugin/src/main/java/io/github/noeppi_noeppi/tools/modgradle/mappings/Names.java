package io.github.noeppi_noeppi.tools.modgradle.mappings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record Names(
        Map<String, String> fields,
        Map<String, String> methods,
        Map<String, String> params) {

    public static final Names EMPTY = new Names(Map.of(), Map.of(), Map.of());

    public Names(Map<String, String> fields, Map<String, String> methods, Map<String, String> params) {
        this.fields = Map.copyOf(fields);
        this.methods = Map.copyOf(methods);
        this.params = Map.copyOf(params);
    }

    public Optional<String> field(String srg) {
        return Optional.ofNullable(this.fields.get(srg));
    }

    public Optional<String> method(String srg) {
        return Optional.ofNullable(this.methods.get(srg));
    }

    public Optional<String> param(String srg) {
        return Optional.ofNullable(this.params.get(srg));
    }
    
    public boolean isEmpty() {
        return this.fields.isEmpty() && this.methods.isEmpty() && this.params.isEmpty();
    }

    public Names merge(Names other) {
        if (other.isEmpty()) return this;
        if (this.isEmpty()) return other;
        Map<String, String> fields = new HashMap<>(this.fields);
        Map<String, String> methods = new HashMap<>(this.methods);
        Map<String, String> params = new HashMap<>(this.params);
        fields.putAll(other.fields);
        methods.putAll(other.methods);
        params.putAll(other.params);
        return new Names(fields, methods, params);
    }
}
