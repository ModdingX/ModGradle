package io.github.noeppi_noeppi.tools.modgradle.mappings.export;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MappingJavadocBuilder {
    
    private static final Pattern REMAP_PATTERN = Pattern.compile("(\\{\\s*@\\s*link\\s+)([Cfm]_\\d+_)(\\s*})");

    public static String remapDoc(String doc, Map<String, String> srgMap) {
        Matcher matcher = REMAP_PATTERN.matcher(doc);
        return matcher.replaceAll(r -> r.group(1) + srgMap.getOrDefault(r.group(2), r.group(2)) + r.group(3));
    }
    
    @Nullable
    public static String buildMethodDoc(@Nullable String baseDoc, Map<String, String> paramDoc) {
        if (baseDoc != null && baseDoc.strip().isEmpty()) baseDoc = null;
        String paramSection = null;
        if (!paramDoc.isEmpty()) {
            int pad = paramDoc.keySet().stream().mapToInt(String::length).max().orElse(-2) + 2;
            StringBuilder sb = new StringBuilder().append("\n");
            for (Map.Entry<String, String> entry : paramDoc.entrySet()) {
                sb.append("@param ");
                sb.append(entry.getKey());
                sb.append(" ".repeat(Math.max(0, pad - entry.getKey().length())));
                sb.append(entry.getValue()
                        .replace("\n", "\n" + " ".repeat("@param ".length() + pad))
                );
                sb.append("\n");
            }
            paramSection = sb.toString();
        }
        if (paramSection == null) return baseDoc;
        if (baseDoc == null) return paramSection;
        StringBuilder sb = new StringBuilder();
        String[] lines = baseDoc.split("\n");
        boolean hasInsertedParam = false;
        for (String line : lines) {
            if (!hasInsertedParam && line.strip().startsWith("@")) {
                hasInsertedParam = true;
                sb.append(paramSection);
            }
            sb.append(line);
            sb.append("\n");
        }
        if (!hasInsertedParam) {
            sb.append(paramSection);
        }
        return sb.toString();
    }
}
