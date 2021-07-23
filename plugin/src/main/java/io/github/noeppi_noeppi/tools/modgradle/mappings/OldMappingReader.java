package io.github.noeppi_noeppi.tools.modgradle.mappings;

import de.siegmar.fastcsv.reader.NamedCsvReader;

import javax.annotation.WillClose;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class OldMappingReader {
    
    public static BaseNames readOldMappings(@WillClose InputStream in, boolean isDoc, boolean unofficial) throws IOException {
        ZipInputStream zin = new ZipInputStream(in);
        Map<String, String> fieldsX = new HashMap<>();
        Map<String, String> methodsX = new HashMap<>();
        Map<String, String> paramsX = new HashMap<>();
        BaseNames base = BaseNames.EMPTY;
        for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
            String name = entry.getName();
            if (name.startsWith("/")) name = name.substring(1);
            switch (name) {
                case "fields.csv" -> addValues(zin, "searge", isDoc ? "desc" : "name", fieldsX);
                case "methods.csv" -> addValues(zin, "searge", isDoc ? "desc" : "name", methodsX);
                case "params.csv" -> { if (!isDoc) addValues(zin, "param", "name", paramsX); }
            }
            if (unofficial && name.equals(".mcp_base")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zin));
                String mcp_base = reader.readLine();
                // Don't close as it would close the entire zip stream
                String channel = mcp_base.substring(0, mcp_base.indexOf('_'));
                String version = mcp_base.substring(mcp_base.indexOf('_') + 1);
                URL url = new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_" + channel + "/" + version + "/mcp_" + channel + "-" + version + ".zip");
                base = readOldMappings(url.openStream(), isDoc, false);
            }
        }
        in.close();
        return base.merge(new BaseNames(
                Collections.unmodifiableMap(fieldsX),
                Collections.unmodifiableMap(methodsX),
                Collections.unmodifiableMap(paramsX)
        ));
    }
    
    private static void addValues(InputStream file, String from, @SuppressWarnings("SameParameterValue") String to, Map<String, String> target) {
        NamedCsvReader csv = NamedCsvReader.builder().build(new InputStreamReader(file));
        if (!to.equalsIgnoreCase("desc") || csv.getHeader().contains("desc")) {
            csv.stream().forEach(row -> target.put(row.getField(from), row.getField(to)));
        }
    }
}
