package org.moddingx.modgradle.util;

import groovy.json.StringEscapeUtils;
import groovy.lang.*;

import java.util.*;
import java.util.stream.Collectors;

public class DynamicObject extends GroovyObjectSupport {

    private static final Set<String> RESERVED_METHODS = Set.of(
            "getAt", "putAt", "isCase", "getProperty", "setProperty", "invokeMethod", "equals", "hashCode", "toString"
    );
    
    private final Map<String, Object> properties;

    public DynamicObject() {
        this.properties = new HashMap<>();
    }

    public DynamicObject(Properties properties) {
        this();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            this.putAt(entry.getKey().toString(), entry.getValue());
        }
    }
    
    public Object getAt(String propertyName) { // this[propertyName]
        return this.getProperty(propertyName);
    }
    
    public void putAt(String propertyName, Object newValue) { // this[propertyName] = newValue
        if (newValue instanceof GString gs) {
            newValue = gs.toString();
        }
        if (newValue == void.class) {
            this.properties.remove(propertyName);
        } else {
            this.properties.put(propertyName, newValue);
        }
    }
    
    public boolean isCase(String propertyName) { // propertyName in this
        return this.properties.containsKey(propertyName);
    }
    
    public Map<String, ?> toMap() {
        return Map.copyOf(this.properties);
    }
    
    @Override
    public Object getProperty(String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            return this.properties.get(propertyName);
        } else {
            throw new MissingPropertyException("Property " + propertyName + " not found in dynamic object.");
        }
    }

    @Override
    public void setProperty(String propertyName, Object newValue) {
        if (this.properties.containsKey(propertyName)) {
            throw new UnsupportedOperationException("Can't change property " + propertyName);
        } else {
            throw new MissingPropertyException("Property " + propertyName + " not found in dynamic object.");
        }
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        if (!RESERVED_METHODS.contains(name) && this.properties.containsKey(name)) {
            Object prop = this.properties.get(name);
            if (prop instanceof Closure<?> closure) {
                return closure.invokeMethod("call", args);
            } else {
                throw new UnsupportedOperationException("Can't invoke " + name + " (" + (prop == null ? null : prop.getClass().getName()) + ")");
            }
        } else {
            return super.invokeMethod(name, args);
        }
    }

    @Override
    public String toString() {
        return this.properties.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof String str) {
                        return key + ": \"" + StringEscapeUtils.escapeJava(str) + "\"";
                    } else if (value instanceof DynamicObject dyn) {
                        return key + ":\n    " + dyn.toString().replace("\n", "\n    ");
                    } else {
                        return key + ": " + value;
                    }
                })
                .collect(Collectors.joining("\n"));
    }
}
