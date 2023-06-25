package org.moddingx.modgradle.util.io.zip;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public sealed abstract class ZipBuilder implements Closeable permits DefaultZipBuilder, OrderingZipBuilder {
    
    private static final long CONSTANT_TIME = new GregorianCalendar(1980, Calendar.FEBRUARY, 1, 0, 0, 0).getTimeInMillis();
    
    private final boolean preserveTimestamps;

    protected ZipBuilder(boolean preserveTimestamps) {
        this.preserveTimestamps = preserveTimestamps;
    }
    
    public static ZipBuilder create(ZipOutputStream out, boolean preserveTimestamps, boolean stableFileOrder) {
        if (stableFileOrder) {
            return new OrderingZipBuilder(out, preserveTimestamps);
        } else {
            return new DefaultZipBuilder(out, preserveTimestamps);
        }
    }
    
    public OutputStream addEntry(String name) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        if (!this.preserveTimestamps) {
            entry.setTime(CONSTANT_TIME);
        }
        return this.doAddEntry(entry);
    }
    
    public OutputStream addEntry(String name, ZipEntry old) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(this.preserveTimestamps ? old.getTime() : CONSTANT_TIME);
        if (this.preserveTimestamps && old.getLastModifiedTime() != null) entry.setLastModifiedTime(old.getLastModifiedTime());
        if (this.preserveTimestamps && old.getLastAccessTime() != null) entry.setLastAccessTime(old.getLastAccessTime());
        if (this.preserveTimestamps && old.getCreationTime() != null) entry.setCreationTime(old.getCreationTime());
        if (old.getComment() != null) entry.setComment(old.getComment());
        return this.doAddEntry(entry);
    }
    
    public OutputStream addEntry(String name, Path old) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        if (this.preserveTimestamps) {
            BasicFileAttributes attr = null;
            try {
                attr = Files.readAttributes(old, BasicFileAttributes.class);
            } catch (UnsupportedOperationException e) {
                //
            }
            if (attr != null) {
                if (attr.lastModifiedTime() != null) entry.setTime(attr.lastModifiedTime().toMillis());
                if (attr.creationTime() != null) entry.setCreationTime(attr.creationTime());
                if (attr.lastAccessTime() != null) entry.setLastAccessTime(attr.lastAccessTime());
                if (attr.lastModifiedTime() != null) entry.setLastModifiedTime(attr.lastModifiedTime());
            }
        } else {
            entry.setTime(CONSTANT_TIME);
        }
        return this.doAddEntry(entry);
    }
    
    protected abstract OutputStream doAddEntry(ZipEntry entry) throws IOException;
}
