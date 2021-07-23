package io.github.noeppi_noeppi.tools.modgradle.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record PackageMatcher(List<String> packages, List<String> but) {
    
    public Predicate<String> getMatcher() {
        Predicate<String> includes = this.packages.stream()
                .map(PackageMatcher::pkgPattern)
                .map(Pattern::asMatchPredicate)
                .reduce(pkg -> false, (p1, p2) -> pkg -> p1.test(pkg) || p2.test(pkg));
        
        Predicate<String> excludes = this.but.stream()
                .map(PackageMatcher::pkgPattern)
                .map(Pattern::asMatchPredicate)
                .reduce(pkg -> false, (p1, p2) -> pkg -> p1.test(pkg) || p2.test(pkg));
        return pkg -> {
            String matchStr = pkg + ".";
            return includes.test(matchStr) && !excludes.test(matchStr);
        };
    }
    
    private static Pattern pkgPattern(String pkg) {
        String pattern = Arrays.stream(pkg.split("\\."))
                .map(part -> switch (part) {
                    case "**" -> ".+";
                    case "*" -> "[^\\.]+";
                    case "*?" -> ".*";
                    default -> Pattern.quote(part);
                })
                .collect(Collectors.joining("\\.")) + "\\.";
        return Pattern.compile(pattern);
    }
}
