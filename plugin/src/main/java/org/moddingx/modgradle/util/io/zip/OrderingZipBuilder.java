package org.moddingx.modgradle.util.io.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class OrderingZipBuilder extends ZipBuilder {
    
    // Jar files should contain META-INF as first entries.
    private static final Comparator<ZipEntry> JAR_ORDER = new Comparator<>() {
        
        @Override
        public int compare(ZipEntry first, ZipEntry second) {
            int prioResult = Integer.compare(priority(first.getName()), priority(second.getName()));
            return prioResult != 0 ? prioResult : first.getName().compareTo(second.getName());
        }
        
        private static int priority(String name) {
            while (name.startsWith("/")) name = name.substring(1);
            return switch (name) {
                case "META-INF", "META-INF/" -> 0;
                case "META-INF/MANIFEST.MF" -> 1;
                default -> name.startsWith("META-INF/") ? 2 : 3;
            };
        }
    };
    
    private final ZipOutputStream out;
    private final List<Entry> entries;
    
    public OrderingZipBuilder(ZipOutputStream out, boolean preserveTimestamps) {
        super(preserveTimestamps);
        this.out = out;
        this.entries = new ArrayList<>();
    }

    @Override
    protected OutputStream doAddEntry(ZipEntry entry) throws IOException {
        ByteArrayOutputStream entryOut = new ByteArrayOutputStream();
        this.entries.add(new Entry(entry, entryOut));
        return entryOut;
    }

    @Override
    public void close() throws IOException {
        // Write elements in stable order
        for (Entry entry : this.entries.stream().sorted(Comparator.comparing(Entry::entry, JAR_ORDER)).toList()) {
            this.out.putNextEntry(entry.entry());
            entry.out().close();
            entry.out().writeTo(this.out);
            this.out.closeEntry();
        }
        this.out.close();
    }
    
    record Entry(ZipEntry entry, ByteArrayOutputStream out) {
        
    }
}
