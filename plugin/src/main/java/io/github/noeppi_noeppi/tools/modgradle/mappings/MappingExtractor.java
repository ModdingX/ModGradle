package io.github.noeppi_noeppi.tools.modgradle.mappings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.INamedMappingFile;

import javax.annotation.WillClose;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MappingExtractor {

    public static SrgInfo extractSrg(@WillClose InputStream in) throws IOException {
        ZipInputStream zin = new ZipInputStream(in);
        IMappingFile srg = null;
        ImmutableList<String> ctor = null;
        ImmutableSet<Integer> statics = null;
        for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
            String name = entry.getName();
            if (name.startsWith("/")) name = name.substring(1);
            switch (name) {
                case "config/joined.tsrg" -> srg = IMappingFile.load(zin);
                case "config/constructors.txt" -> ctor = new BufferedReader(new InputStreamReader(zin)).lines()
                        .collect(ImmutableList.toImmutableList());
                case "config/static_methods.txt" -> statics = new BufferedReader(new InputStreamReader(zin)).lines()
                        .map(SrgRemapper.OLD_SRG_M::matcher)
                        .filter(Matcher::matches)
                        .map(m -> Integer.parseInt(m.group(1)))
                        .collect(ImmutableSet.toImmutableSet());
            }
        }
        if (srg == null) {
            throw new IllegalStateException("No SRG file found.");
        }
        if (ctor == null) {
            throw new IllegalStateException("No constructor information found.");
        }
        if (statics == null) {
            throw new IllegalStateException("No static method information found.");
        }
        in.close();
        return new SrgInfo(srg, ctor, statics);
    }

    public static INamedMappingFile extractSrg2(@WillClose InputStream in) throws IOException {
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
    
    public static INamedMappingFile extractTiny(@WillClose InputStream in) throws IOException {
        ZipInputStream zin = new ZipInputStream(in);
        for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
            String name = entry.getName();
            if (name.startsWith("/")) name = name.substring(1);
            if ("mappings/mappings.tiny".equals(name)) {
                INamedMappingFile tiny = INamedMappingFile.load(zin);
                zin.close();
                return tiny;
            }
        }
        in.close();
        throw new IllegalStateException("No SRG2 file found.");
    }
    
    public static record SrgInfo(
            IMappingFile srg,
            ImmutableList<String> ctors,
            ImmutableSet<Integer> statics
    ) { }
}
