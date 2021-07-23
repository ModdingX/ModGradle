package io.github.noeppi_noeppi.tools.modgradle.mappings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record BaseNames(
        Map<String, String> fields,
        Map<String, String> methods,
        Map<String, String> params
) {

    public static final BaseNames EMPTY = new BaseNames(Map.of(), Map.of(), Map.of());

    public BaseNames merge(BaseNames other) {
        Map<String, String> fields = new HashMap<>(this.fields);
        Map<String, String> methods = new HashMap<>(this.methods);
        Map<String, String> params = new HashMap<>(this.params);
        fields.putAll(other.fields());
        methods.putAll(other.methods());
        params.putAll(other.params());
        return new BaseNames(
                Collections.unmodifiableMap(fields),
                Collections.unmodifiableMap(methods),
                Collections.unmodifiableMap(params)
        );
    }
}
