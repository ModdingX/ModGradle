package io.github.noeppi_noeppi.tools.modgradle.mappings.export;

import de.siegmar.fastcsv.writer.CsvWriter;
import io.github.noeppi_noeppi.tools.modgradle.mappings.BaseNames;
import io.github.noeppi_noeppi.tools.modgradle.mappings.Names;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;

import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class OldMcpExporter {

    // SRG is required for params
    public static void writeMcpZip(@WillClose OutputStream out, INamedMappingFile srg, Names names, Names doc) throws IOException {
        IMappingFile srgMap = srg.getMap("obf", "srg");
        IMappingFile idMap = srg.getMap("obf", "id");
        
        Map<String, String> params = new HashMap<>();
        Map<String, Map<String, String>> paramDoc = new HashMap<>();
        srgMap.getClasses().stream().flatMap(cls -> cls.getMethods().stream())
                .filter(m -> !"<clinit>".equals(m.getMapped()))
                .forEach(method -> {
                    String baseName = null;
                    if ("<init>".equals(method.getMapped())) {
                        IMappingFile.IClass idClass = idMap.getClass(method.getParent().getOriginal());
                            if (idClass != null) {
                                IMappingFile.IMethod idMethod = idClass.getMethod(method.getOriginal(), method.getDescriptor());
                                if (idMethod != null) {
                                    baseName = "i_" + idMethod.getMapped() + "_";
                                }
                            }
                    } else {
                        baseName = method.getMapped();
                    }
                    if (baseName != null && (names.params().containsKey(baseName) || doc.params().containsKey(baseName))) {
                        Map<Integer, String> pnameMap = names.params().getOrDefault(baseName, Map.of());
                        Map<Integer, String> pdocMap = doc.params().getOrDefault(baseName, Map.of());
                        String effectiveFinalBaseName = baseName;
                        method.getParameters().forEach(param -> {
                            if (pnameMap.containsKey(param.getIndex())) params.put(param.getMapped(), pnameMap.get(param.getIndex()));
                            if (pdocMap.containsKey(param.getIndex())) paramDoc.computeIfAbsent(effectiveFinalBaseName, k-> new HashMap<>()).put(params.getOrDefault(param.getMapped(), param.getMapped()), pdocMap.get(param.getIndex()));
                        });
                    }
                });
        
        BaseNames baseNames = new BaseNames(names.fields(), names.methods(), Collections.unmodifiableMap(params));
        
        Map<String, String> plainSrgMap = new HashMap<>();
        plainSrgMap.putAll(names.classes());
        plainSrgMap.putAll(names.fields());
        plainSrgMap.putAll(names.methods());
        
        Map<String, String> docMap = new HashMap<>();
        
        for (Map.Entry<String, String> entry : doc.fields().entrySet()) {
            docMap.put(entry.getKey(), MappingJavadocBuilder.remapDoc(entry.getValue(), plainSrgMap));
        }

        Set<String> methodsWithDoc = new HashSet<>();
        methodsWithDoc.addAll(doc.methods().keySet());
        methodsWithDoc.addAll(paramDoc.keySet().stream().filter(k -> !k.startsWith("i_")).collect(Collectors.toSet()));
        for (String key : methodsWithDoc) {
            String builtDoc = MappingJavadocBuilder.buildMethodDoc(doc.methods().getOrDefault(key, null), paramDoc.getOrDefault(key, Map.of()));
            if (builtDoc != null) {
                docMap.put(key, MappingJavadocBuilder.remapDoc(builtDoc, plainSrgMap));
            }
        }
        
        writeMcpZip(out, baseNames, docMap);
    }
    
    public static void writeMcpZip(@WillClose OutputStream out, BaseNames names, BaseNames doc) throws IOException {
        writeMcpZip(out, names, doc, null);
    }
    
    public static void writeMcpZip(@WillClose OutputStream out, BaseNames names, BaseNames doc, @Nullable Map<String, String> classDoc) throws IOException {
        Map<String, String> srgDoc = new HashMap<>();
        srgDoc.putAll(doc.fields());
        srgDoc.putAll(doc.methods());
        srgDoc.putAll(doc.params());
        writeMcpZip(out, names, srgDoc, classDoc);
    }
    
    public static void writeMcpZip(@WillClose OutputStream out, BaseNames names, Map<String, String> srgDoc) throws IOException {
        writeMcpZip(out, names, srgDoc, null);
    }
    
    public static void writeMcpZip(@WillClose OutputStream out, BaseNames names, Map<String, String> srgDoc, @Nullable Map<String, String> classDoc) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        
        zip.putNextEntry(new ZipEntry("fields.csv"));
        writeCsvFile(zip, csv -> {
            csv.writeRow("searge", "name", "side", "desc");
            for (Map.Entry<String, String> entry : names.fields().entrySet()) {
                csv.writeRow(entry.getKey(), entry.getValue(), "2", srgDoc.getOrDefault(entry.getKey(), ""));
            }
        });
        
        zip.putNextEntry(new ZipEntry("methods.csv"));
        writeCsvFile(zip, csv -> {
            csv.writeRow("searge", "name", "side", "desc");
            for (Map.Entry<String, String> entry : names.methods().entrySet()) {
                csv.writeRow(entry.getKey(), entry.getValue(), "2", srgDoc.getOrDefault(entry.getKey(), ""));
            }
        });
        
        zip.putNextEntry(new ZipEntry("params.csv"));
        writeCsvFile(zip, csv -> {
            csv.writeRow("param", "name", "side");
            for (Map.Entry<String, String> entry : names.params().entrySet()) {
                csv.writeRow(entry.getKey(), entry.getValue(), "2");
            }
        });
        
        if (classDoc != null) {
            zip.putNextEntry(new ZipEntry("classes.csv"));
            writeCsvFile(zip, csv -> {
                csv.writeRow("searge", "name", "side", "desc");
                for (Map.Entry<String, String> entry : classDoc.entrySet()) {
                    csv.writeRow(entry.getKey(), entry.getKey(), "2", entry.getValue());
                }
            });
        }
        
        zip.close();
        out.close();
    }
    
    private static void writeCsvFile(OutputStream out, CsvAction action) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bout));
        CsvWriter csv = CsvWriter.builder().build(writer);
        action.perform(csv);
        csv.close();
        writer.close();
        bout.close();
        out.write(bout.toByteArray());
    }
    
    private interface CsvAction {
        void perform(CsvWriter writer) throws IOException;
    }
}
