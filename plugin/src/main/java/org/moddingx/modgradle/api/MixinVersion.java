package org.moddingx.modgradle.api;

/**
 * A mixin version is defined through the compatibility level and the minimum required version.
 */
public record MixinVersion(String compatibility, String release) {}
