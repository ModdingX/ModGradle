package org.moddingx.modgradle.plugins.meta;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import groovy.lang.ReadOnlyPropertyException;

public class ModPropertyAccess extends GroovyObjectSupport {

    private final ModExtension extension;

    public ModPropertyAccess(ModExtension extension) {
        this.extension = extension;
    }

    public Object getAt(String propertyName) { // this[propertyName]
        return this.extension.getAt(propertyName);
    }

    public boolean isCase(String propertyName) { // propertyName in this
        return this.extension.isCase(propertyName);
    }
    
    @Override
    public Object getProperty(String propertyName) {
        if (this.extension.isCase(propertyName)) {
            return this.extension.getAt(propertyName);
        } else {
            throw new MissingPropertyException(propertyName, this.getClass());
        }
    }

    @Override
    public void setProperty(String propertyName, Object newValue) {
        if (this.extension.isCase(propertyName)) {
            throw new ReadOnlyPropertyException(propertyName, this.getClass());
        } else {
            throw new MissingPropertyException(propertyName, this.getClass());
        }
    }
}
