package org.moddingx.modgradle.util.io.zip;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DefaultZipBuilder extends ZipBuilder {
    
    private final ZipOutputStream out;
    
    public DefaultZipBuilder(ZipOutputStream out, boolean preserveTimestamps) {
        super(preserveTimestamps);
        this.out = out;
    }

    @Override
    protected OutputStream doAddEntry(ZipEntry entry) throws IOException {
        this.out.putNextEntry(entry);
        return new NonClosingStream(this.out);
    }

    @Override
    public void close() throws IOException {
        this.out.close();
    }
    
    private static class NonClosingStream extends FilterOutputStream {

        private final ZipOutputStream zipOut;
        private boolean closed;
        
        public NonClosingStream(ZipOutputStream out) {
            super(out);
            this.zipOut = out;
        }

        @Override
        public synchronized void close() throws IOException {
            if (!this.closed) {
                this.zipOut.closeEntry();
                this.closed = true;
            }
        }
    }
}
