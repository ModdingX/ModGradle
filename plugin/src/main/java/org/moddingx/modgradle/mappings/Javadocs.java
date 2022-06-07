package org.moddingx.modgradle.mappings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record Javadocs(
        Map<String, String> packages,
        Map<String, String> classes,
        Map<String, String> fields,
        Map<String, String> methods) {

    public static final Javadocs EMPTY = new Javadocs(Map.of(), Map.of(), Map.of(), Map.of());

    public Javadocs(Map<String, String> packages, Map<String, String> classes, Map<String, String> fields, Map<String, String> methods) {
        this.packages = Map.copyOf(packages);
        this.classes = Map.copyOf(classes);
        this.fields = Map.copyOf(fields);
        this.methods = Map.copyOf(methods);
    }

    public Optional<String> pkg(String internalName) {
        return Optional.ofNullable(this.packages.get(internalName));
    }

    public Optional<String> cls(String internalName) {
        return Optional.ofNullable(this.classes.get(internalName));
    }

    public Optional<String> field(String srg) {
        return Optional.ofNullable(this.fields.get(srg));
    }

    public Optional<String> method(String srg) {
        return Optional.ofNullable(this.methods.get(srg));
    }

    public boolean isEmpty() {
        return this.packages.isEmpty() && this.classes.isEmpty() && this.fields.isEmpty() && this.methods.isEmpty();
    }
    
    public Javadocs merge(Javadocs other) {
        if (other.isEmpty()) return this;
        if (this.isEmpty()) return other;
        Map<String, String> packages = new HashMap<>(this.packages);
        Map<String, String> classes = new HashMap<>(this.classes);
        Map<String, String> fields = new HashMap<>(this.fields);
        Map<String, String> methods = new HashMap<>(this.methods);
        packages.putAll(other.packages);
        classes.putAll(other.classes);
        fields.putAll(other.fields);
        methods.putAll(other.methods);
        return new Javadocs(packages, classes, fields, methods);
    }
}
