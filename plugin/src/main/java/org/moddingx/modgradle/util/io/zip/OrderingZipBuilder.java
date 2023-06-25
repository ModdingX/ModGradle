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
        for (Entry entry : this.entries.stream().sorted(Comparator.comparing((Entry e) -> e.entry().getName())).toList()) {
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
