package io.github.noeppi_noeppi.tools.modgradle.mappings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.noeppi_noeppi.tools.modgradle.ModGradle;
import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;

import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ParchmentLoader {

    public static IMappingFile loadParchment(IMappingFile official, @WillClose InputStream in, IMappingFile srg, Set<Integer> statics) throws IOException {
        JsonObject parchment = extractParchment(in);
        IMappingBuilder builder = IMappingBuilder.create("from", "to");
        Map<String, JsonObject> clsObjMap = new HashMap<>();
        for (JsonElement clsElem : parchment.getAsJsonArray("classes")) {
            JsonObject cls = clsElem.getAsJsonObject();
            String name = cls.get("name").getAsString();
            clsObjMap.put(name, cls);
        }
        official.getClasses().forEach(cls -> processClass(builder, cls, clsObjMap.getOrDefault(cls.getMapped(), null), srg, statics));
        return builder.build().getMap("from", "to");
    }

    private static void processClass(IMappingBuilder mappings, IMappingFile.IClass cls, @Nullable JsonObject json, IMappingFile srg, Set<Integer> statics) {
        IMappingBuilder.IClass builder = mappings.addClass(cls.getOriginal(), cls.getMapped());
        IMappingFile.IClass srgClass = srg.getClass(cls.getOriginal());
        Map<String, JsonObject> methodObjMap = new HashMap<>();
        if (json != null && json.has("methods")) {
            for (JsonElement methodElem : json.getAsJsonArray("methods")) {
                JsonObject method = methodElem.getAsJsonObject();
                String id = method.get("name").getAsString() + method.get("descriptor").getAsString();
                methodObjMap.put(id, method);
            }
        }
        cls.getFields().forEach(field -> builder.field(field.getOriginal(), field.getMapped()));
        cls.getMethods().forEach(method -> {
            IMappingBuilder.IMethod methodBuilder = builder.method(method.getDescriptor(), method.getOriginal(), method.getMapped());
            JsonObject methodData = methodObjMap.getOrDefault(method.getMapped() + method.getMappedDescriptor(), null);
            if (methodData != null && methodData.has("parameters")) {
                for (JsonElement paramElem : methodData.getAsJsonArray("parameters")) {
                    JsonObject paramData = paramElem.getAsJsonObject();
                    int bytecodeIdx = paramData.get("index").getAsInt();
                    IMappingFile.IMethod srgMethod = srgClass == null ? null : srgClass.getMethod(method.getOriginal(), method.getDescriptor());
                    Matcher matcher = srgMethod == null ? null : SrgRemapper.OLD_SRG_M.matcher(srgMethod.getMapped());
                    Integer methodId = null;
                    if (matcher != null && matcher.matches()) methodId = Integer.parseInt(matcher.group(1));
                    boolean syntheticThis = "<init>".equals(method.getMapped()) || (methodId == null || !statics.contains(methodId));
                    int idx = getRealIdx(bytecodeIdx, syntheticThis, method.getMappedDescriptor());
                    if (idx >= 0 && paramData.has("name")) {
                        methodBuilder.parameter(idx, "o" + idx, paramData.get("name").getAsString());
                    }
                }
            }
        });
    }

    private static JsonObject extractParchment(@WillClose InputStream in) throws IOException {
        ZipInputStream zin = new ZipInputStream(in);
        for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
            String name = entry.getName();
            if (name.startsWith("/")) name = name.substring(1);
            if ("parchment.json".equals(name)) {
                Reader reader = new InputStreamReader(zin);
                JsonObject json = ModGradle.INTERNAL.fromJson(reader, JsonObject.class);
                reader.close();
                zin.close();
                return json;
            }
        }
        in.close();
        throw new IllegalStateException("No parchemnt.json file found.");
    }

    private static int getRealIdx(int bytecodeIdx, boolean syntheticThis, String signature) {
        int counter = syntheticThis ? 1 : 0;
        int idx = 0;
        for (char c : reducedArgString(signature).toCharArray()) {
            if (counter >= bytecodeIdx) {
                return idx;
            } else {
                counter += (c == 'J' || c == 'D') ? 2 : 1;
                idx += 1;
            }
        }
        return -1;
    }

    private static String reducedArgString(String signature) {
        StringBuilder sb = new StringBuilder();
        boolean skipping = false;
        for (char c : signature.toCharArray()) {
            if (c == ';') {
                skipping = false;
            } else if (!skipping && c != '(') {
                if (c == ')') {
                    return sb.toString();
                } else {
                    sb.append(c);
                    if (c == 'L') skipping = true;
                }
            }
        }
        return sb.toString();
    }
}
