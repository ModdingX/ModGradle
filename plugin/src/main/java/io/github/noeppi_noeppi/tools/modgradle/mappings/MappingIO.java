package io.github.noeppi_noeppi.tools.modgradle.mappings;

import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.minecraftforge.srgutils.INamedMappingFile;

import javax.annotation.Nullable;
import javax.annotation.WillClose;
import javax.annotation.WillNotClose;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MappingIO {

    public static INamedMappingFile readMcpConfigSrg(@WillClose InputStream in) throws IOException {
        ZipInputStream zin = new ZipInputStream(in);
        for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
            String name = entry.getName();
            if (name.startsWith("/")) name = name.substring(1);
            if ("config/joined.tsrg".equals(name)) {
                INamedMappingFile srg = INamedMappingFile.load(zin);
                zin.close();
                return srg;
            }
        }
        in.close();
        throw new IllegalStateException("No SRG2 file found.");
    }

    public static NameMappings readNames(@WillClose InputStream in) throws IOException {
        return readNames(in, false);
    }
    
    public static NameMappings readNames(@WillClose InputStream in, boolean unofficial) throws IOException {
        ZipInputStream zin = new ZipInputStream(in);
        
        Map<String, String> fields = new HashMap<>();
        Map<String, String> methods = new HashMap<>();
        Map<String, String> params = new HashMap<>();

        Map<String, String> packageDoc = new HashMap<>();
        Map<String, String> classDoc = new HashMap<>();
        Map<String, String> fieldDoc = new HashMap<>();
        Map<String, String> methodDoc = new HashMap<>();
        
        Names base = Names.EMPTY;
        Javadocs baseDoc = Javadocs.EMPTY;
        for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
            String name = entry.getName();
            if (name.startsWith("/")) name = name.substring(1);
            switch (name) {
                case "packages.csv" -> addMappingValues(zin, "searge", null, packageDoc, true);
                case "classes.csv" -> addMappingValues(zin, "searge", null, classDoc, true);
                case "fields.csv" -> addMappingValues(zin, "searge", fields, fieldDoc, false);
                case "methods.csv" -> addMappingValues(zin, "searge", methods, methodDoc, false);
                case "params.csv" -> addMappingValues(zin, "param", params, null, false);
            }
            if (unofficial && name.equals(".mcp_base")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zin));
                String mcp_base = reader.readLine();
                // Don't close as it would close the entire zip stream
                String channel = mcp_base.substring(0, mcp_base.indexOf('_'));
                String version = mcp_base.substring(mcp_base.indexOf('_') + 1);
                URL url = new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_" + channel + "/" + version + "/mcp_" + channel + "-" + version + ".zip");
                NameMappings nm = readNames(url.openStream(), false);
                base = nm.names();
                baseDoc = nm.docs();
            }
        }
        in.close();
        return new NameMappings(
                base.merge(new Names(fields, methods, params)),
                baseDoc.merge(new Javadocs(packageDoc, classDoc, fieldDoc, methodDoc))
        );
    }

    private static void addMappingValues(@WillNotClose InputStream file, String from, @Nullable Map<String, String> target, @Nullable Map<String, String> docTarget, boolean replaceName) {
        NamedCsvReader csv = NamedCsvReader.builder().build(new InputStreamReader(file));
        if (target != null && csv.getHeader().contains("name")) {
            csv.stream().forEach(row -> target.put(replaceInput(row.getField(from), replaceName), replaceInput(row.getField("name"), replaceName)));
        }
        if (docTarget != null && csv.getHeader().contains("desc")) {
            csv.stream().forEach(row -> docTarget.put(replaceInput(row.getField(from), replaceName), row.getField("desc")));
        }
    }
    
    private static String replaceInput(String str, boolean replace) {
        return replace ? str.replace('.', '/') : str;
    }

    public static void writeMappings(@WillClose OutputStream out, Names names) throws IOException {
        writeMappings(out, names, Javadocs.EMPTY);
    }

    public static void writeMappings(@WillClose OutputStream out, Names names, Javadocs docs) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);

        zip.putNextEntry(new ZipEntry("fields.csv"));
        writeCsvFile(zip, csv -> {
            csv.writeRow("searge", "name", "side", "desc");
            for (Map.Entry<String, String> entry : names.fields().entrySet()) {
                csv.writeRow(entry.getKey(), entry.getValue(), "2", docs.field(entry.getKey()).orElse(""));
            }
        });

        zip.putNextEntry(new ZipEntry("methods.csv"));
        writeCsvFile(zip, csv -> {
            csv.writeRow("searge", "name", "side", "desc");
            for (Map.Entry<String, String> entry : names.methods().entrySet()) {
                csv.writeRow(entry.getKey(), entry.getValue(), "2", docs.method(entry.getKey()).orElse(""));
            }
        });

        zip.putNextEntry(new ZipEntry("params.csv"));
        writeCsvFile(zip, csv -> {
            csv.writeRow("param", "name", "side");
            for (Map.Entry<String, String> entry : names.params().entrySet()) {
                csv.writeRow(entry.getKey(), entry.getValue(), "2");
            }
        });

        if (!docs.packages().isEmpty()) {
            zip.putNextEntry(new ZipEntry("packages.csv"));
            writeCsvFile(zip, csv -> {
                csv.writeRow("searge", "name", "side", "desc");
                for (Map.Entry<String, String> entry : docs.packages().entrySet()) {
                    csv.writeRow(entry.getKey().replace('/', '.'), entry.getKey().replace('/', '.'), "2", entry.getValue());
                }
            });
        }
        
        if (!docs.classes().isEmpty()) {
            zip.putNextEntry(new ZipEntry("classes.csv"));
            writeCsvFile(zip, csv -> {
                csv.writeRow("searge", "name", "side", "desc");
                for (Map.Entry<String, String> entry : docs.classes().entrySet()) {
                    csv.writeRow(entry.getKey().replace('/', '.'), entry.getKey().replace('/', '.'), "2", entry.getValue());
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
    
    public record NameMappings(Names names, Javadocs docs) {}
    
    private interface CsvAction {
        void perform(CsvWriter writer) throws IOException;
    }
}
