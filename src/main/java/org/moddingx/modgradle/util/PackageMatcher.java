package org.moddingx.modgradle.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class PackageMatcher implements Predicate<String> {
    
    private final List<String> packages;
    private final List<String> but;
    private final Predicate<String> test;
    
    public PackageMatcher(List<String> packages, List<String> but) {
        this.packages = List.copyOf(packages);
        this.but = List.copyOf(but);
        Predicate<String> includes = this.packages.stream()
                .map(PackageMatcher::pkgPattern)
                .map(Pattern::asMatchPredicate)
                .reduce(pkg -> false, (p1, p2) -> pkg -> p1.test(pkg) || p2.test(pkg));
        Predicate<String> excludes = this.but.stream()
                .map(PackageMatcher::pkgPattern)
                .map(Pattern::asMatchPredicate)
                .reduce(pkg -> false, (p1, p2) -> pkg -> p1.test(pkg) || p2.test(pkg));
        this.test = pkg -> {
            String matchStr = "." + pkg;
            return includes.test(matchStr) && !excludes.test(matchStr);
        };
    }

    @Override
    public boolean test(String pkg) {
        return this.test.test(pkg);
    }

    private static Pattern pkgPattern(String pkg) {
        String pattern = Arrays.stream(pkg.split("\\."))
                .map(part -> switch (part) {
                    case "**" -> "\\..+";
                    case "*" -> "\\.[^\\.]+";
                    case "*?" -> "\\.?.*";
                    default -> Pattern.quote("." + part);
                })
                .collect(Collectors.joining(""));
        return Pattern.compile(pattern);
    }

    public List<String> packages() {
        return this.packages;
    }

    public List<String> but() {
        return this.but;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof PackageMatcher other) {
            return Objects.equals(this.packages, other.packages) && Objects.equals(this.but, other.but);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.packages, this.but);
    }
}
