package net.ravendb.client.documents.operations.replication;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

class PullReplicationPathFilterUtils {

    private PullReplicationPathFilterUtils() {
    }

    public static String[] normalizeAndValidate(String[] allowedPaths, String name) {
        String[] normalizedPaths = normalize(allowedPaths);
        validate(normalizedPaths, name);
        return normalizedPaths;
    }

    public static String[] normalize(String[] allowedPaths) {
        if (allowedPaths == null) {
            return null;
        }

        List<String> normalized = null;

        for (String path : allowedPaths) {
            String normalizedPath = path != null ? path.trim() : null;
            if (StringUtils.isEmpty(normalizedPath)) {
                continue;
            }

            if (normalized == null) {
                normalized = new ArrayList<>(allowedPaths.length);
            }
            normalized.add(normalizedPath);
        }

        return normalized != null ? normalized.toArray(new String[0]) : new String[0];
    }

    private static void validate(String[] allowedPaths, String name) {
        if (allowedPaths == null || allowedPaths.length == 0) {
            return;
        }

        for (String path : allowedPaths) {
            if (path.charAt(path.length() - 1) != '*') {
                continue;
            }

            if (path.length() > 1 && path.charAt(path.length() - 2) != '/' && path.charAt(path.length() - 2) != '-') {
                throw new IllegalStateException(
                        "When using '*' at the end of the allowed path, the previous character must be '/' or '-', but got: " + path + " for " + name);
            }
        }
    }
}
