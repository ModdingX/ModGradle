package io.github.noeppi_noeppi.tools.modgradle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.function.Supplier;

public class ModGradle {

    public static final String SOURCE_TRANSFORM = "io.github.noeppi_noeppi.tools:SourceTransform:1.0.2:fatjar";

    // Target minecraft version. Acts as default
    // ModGradle can still be used with other minecraft versions
    // For example this is the fallback when using an unknown
    // version in the Versioning class
    public static final String TARGET_MINECRAFT = "1.17.1";

    // Target java version for ModGradle and external tools
    // not for the toolchain
    public static final int TARGET_JAVA = 16;

    @SuppressWarnings("TrivialFunctionalExpressionUsage")
    public static final Gson GSON = ((Supplier<Gson>) () -> {
        GsonBuilder gsonbuilder = new GsonBuilder();
        gsonbuilder.disableHtmlEscaping();
        gsonbuilder.setLenient();
        gsonbuilder.setPrettyPrinting();
        return gsonbuilder.create();
    }).get();

    @SuppressWarnings("TrivialFunctionalExpressionUsage")
    public static final Gson INTERNAL = ((Supplier<Gson>) () -> {
        GsonBuilder gsonbuilder = new GsonBuilder();
        gsonbuilder.disableHtmlEscaping();
        return gsonbuilder.create();
    }).get();
}
