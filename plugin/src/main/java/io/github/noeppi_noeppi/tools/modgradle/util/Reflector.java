package io.github.noeppi_noeppi.tools.modgradle.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Reflector {
    
    public static Object callScala(MethodGetter method, Object... params) {
        try {
             return method.get().invoke(null, params);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            } else if (e.getTargetException() instanceof Error) {
                throw (Error) e.getTargetException();
            } else {
                throw new RuntimeException(e.getTargetException());
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access parser class for module file", e);
        }
    }

    @FunctionalInterface
    public interface MethodGetter {
        
        Method get() throws ReflectiveOperationException;
    }
}
