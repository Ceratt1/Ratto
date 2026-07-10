package com.learnia.performanceanalyzer.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class ReferencePolicy {

    private static final Set<String> TRACKING_PARAMETERS = Set.of(
            "fbclid",
            "gclid",
            "mc_cid",
            "mc_eid",
            "si");

    private ReferencePolicy() {
    }

    static Optional<String> normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return Optional.empty();
        }
        try {
            URI source = new URI(rawUrl.trim());
            String scheme = source.getScheme() == null ? "" : source.getScheme().toLowerCase(Locale.ROOT);
            String host = source.getHost() == null ? "" : source.getHost().toLowerCase(Locale.ROOT);
            if (!("https".equals(scheme) || "http".equals(scheme)) || host.isBlank() || source.getUserInfo() != null) {
                return Optional.empty();
            }
            if (isSearchResultsPage(host, source.getPath())) {
                return Optional.empty();
            }
            String query = stripTrackingParameters(source.getRawQuery());
            String authority = source.getPort() < 0 ? host : host + ":" + source.getPort();
            String path = source.getRawPath() == null ? "" : source.getRawPath();
            URI normalized = new URI(scheme + "://" + authority + path + (query == null ? "" : "?" + query))
                    .normalize();
            return Optional.of(normalized.toASCIIString());
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }
    }

    private static boolean isSearchResultsPage(String host, String path) {
        String normalizedPath = path == null ? "" : path.toLowerCase(Locale.ROOT);
        return (host.equals("google.com") || host.endsWith(".google.com"))
                && normalizedPath.startsWith("/search")
                || (host.equals("youtube.com") || host.endsWith(".youtube.com"))
                && normalizedPath.startsWith("/results");
    }

    private static String stripTrackingParameters(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        String filtered = Arrays.stream(rawQuery.split("&"))
                .filter(parameter -> !isTrackingParameter(parameter))
                .collect(Collectors.joining("&"));
        return filtered.isBlank() ? null : filtered;
    }

    private static boolean isTrackingParameter(String parameter) {
        String name = parameter.split("=", 2)[0].toLowerCase(Locale.ROOT);
        return name.startsWith("utm_") || TRACKING_PARAMETERS.contains(name);
    }
}
