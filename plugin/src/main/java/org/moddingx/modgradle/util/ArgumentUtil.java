package org.moddingx.modgradle.util;

import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArgumentUtil {

    public static List<String> replaceArgs(List<String> args, Map<String, List<?>> replacements) {
        if (replacements.isEmpty()) return args;
        ArrayList<String> newArgs = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("{") && arg.endsWith("}")) {
                String id = arg.substring(1, arg.length() - 1);
                if (replacements.containsKey(id)) {
                    List<?> values = replacements.get(id);
                    if (values.isEmpty()) {
                        if (newArgs.isEmpty()) throw new IllegalArgumentException("None-Argument substitution on empty list with key " + id);
                        newArgs.remove(newArgs.size() - 1);
                    } else if (values.size() == 1) {
                        newArgs.add(toArgString(values.get(0)));
                    } else {
                        if (newArgs.isEmpty()) throw new IllegalArgumentException("Multi-Argument substitution on empty list with key " + id);
                        String rep = newArgs.get(newArgs.size() - 1);
                        newArgs.remove(newArgs.size() - 1);
                        for (Object value : values) {
                            newArgs.add(rep);
                            newArgs.add(toArgString(value));
                        }
                    }
                } else {
                    newArgs.add(arg);
                }
            } else {
                newArgs.add(arg);
            }
        }
        return newArgs;
    }

    public static String toArgString(Object value) {
        if (value instanceof String str) {
            return str;
        }  else if (value instanceof Provider<?> provider) {
            return toArgString(provider.get());
        }  else if (value instanceof File file) {
            return file.toPath().toAbsolutePath().normalize().toString();
        } else if (value instanceof Path  path) {
            return path.toAbsolutePath().normalize().toString();
        } else if (value instanceof FileCollection fc) {
            return fc.getAsPath();
        } else if (value instanceof RegularFile file) {
            return file.getAsFile().toPath().toAbsolutePath().normalize().toString();
        } else if (value instanceof Directory dir) {
            return dir.getAsFile().toPath().toAbsolutePath().normalize().toString();
        } else {
            return value.toString();
        }
    }
}
