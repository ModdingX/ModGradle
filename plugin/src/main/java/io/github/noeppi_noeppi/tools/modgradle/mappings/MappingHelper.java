package io.github.noeppi_noeppi.tools.modgradle.mappings;

import io.github.noeppi_noeppi.tools.modgradle.util.IOUtil;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Map;
import java.util.Optional;

public class MappingHelper {

    public static Optional<String> transformMethodSignature(String signature, Map<String, String> classMap) {
        PushbackReader r = new PushbackReader(new StringReader(signature));
        StringBuilder s = new StringBuilder().append("(");
        try {
            IOUtil.skipWhitespace(r);
            if (r.read() != '(') return Optional.empty();
            loop: while (true) {
                IOUtil.skipWhitespace(r);
                int read = r.read();
                switch (read) {
                    case -1 -> { return Optional.empty(); }
                    case ')' -> { s.append(')'); break loop; }
                    case 'L' -> {
                        String cls = IOUtil.readUntil(r, ';');
                        s.append('L').append(classMap.getOrDefault(cls, cls)).append(';');
                    }
                    default -> s.append((char) read);
                }
            }
            IOUtil.skipWhitespace(r);
            loop: while (true) {
                IOUtil.skipWhitespace(r);
                int read = r.read();
                switch (read) {
                    case -1 -> { return Optional.empty(); }
                    case '[' -> s.append('[');
                    case 'L' -> {
                        String cls = IOUtil.readUntil(r, ';');
                        s.append('L').append(classMap.getOrDefault(cls, cls)).append(';');
                    }
                    default -> { s.append((char) read); break loop; }
                }
            }
            return Optional.of(s.toString());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static String getSimplifiedClassSrg(String cls) {
        String result = cls.contains("$") ? cls.substring(cls.lastIndexOf('$') + 1) : cls;
        try {
            Integer.parseInt(result);
            // Did not fail, class is an anonymous class, does not need to be named.
            // Return original name
            return cls;
        } catch (NumberFormatException e) {
            return result;
        }
    }
}
