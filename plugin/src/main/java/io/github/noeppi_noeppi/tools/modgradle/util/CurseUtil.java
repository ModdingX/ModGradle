package io.github.noeppi_noeppi.tools.modgradle.util;

import io.github.noeppi_noeppi.tools.cursewrapper.api.CurseWrapper;

import java.net.URI;

public class CurseUtil {

    public static final CurseWrapper API = new CurseWrapper(URI.create("https://curse.melanx.de/"));
    public static final URI CURSE_MAVEN = URI.create("https://cfa2.cursemaven.com");

    public static URI curseMaven(String endpoint) {
        return CURSE_MAVEN.resolve(endpoint.startsWith("/") ? endpoint : "/" + endpoint);
    }
}
