package io.github.noeppi_noeppi.tools.modgradle.util;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

public class IOUtil {
    
    public static String readUntil(Reader reader, char chr) throws IOException {
        String str = readTo(reader, chr);
        return str.isEmpty() ? "" : str.substring(0, str.length() - 1);
    }
    
    public static String readTo(Reader reader, char chr) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int read = reader.read();
            if (read == -1) return sb.toString();
            sb.append((char) read);
            if ((char) read == chr) return sb.toString();
        }
    }
    
    public static void skipWhitespace(PushbackReader reader) throws IOException {
        while (true) {
            int read = reader.read();
            if (read == -1) return;
            if (!Character.isWhitespace((char) read)) {
                reader.unread(read);
                return;
            }
        }
    }
}
