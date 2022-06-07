package org.moddingx.modgradle.util;

import java.util.function.Predicate;

public class StringUtil {

    public static int indexWhere(String str, Predicate<Character> condition) {
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (condition.test(chars[i])) {
                return i;
            }
        }
        return chars.length;
    }
    
    public static int lastIndexWhere(String str, Predicate<Character> condition) {
        char[] chars = str.toCharArray();
        for (int i = chars.length - 1; i >= 0; i--) {
            if (condition.test(chars[i])) {
                return i;
            }
        }
        return 0;
    }

    public static String quote(String str) {
        return "\"" + str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace(" ", "\\ ") + "\"";
    }
}
