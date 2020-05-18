package net.ravendb.client.documents.indexes;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class IndexDefinitionHelper {
    public static IndexType detectStaticIndexType(String map, String reduce) {

        if (map.isEmpty()) {
            throw new IllegalArgumentException("Index definitions contains no Maps");
        }

        map = stripComments(map);
        map = unifyWhiteSpace(map);

        String mapLower = map.toLowerCase();
        if (mapLower.startsWith("from") || mapLower.startsWith("docs") || mapLower.startsWith("timeseries") || mapLower.startsWith("counters")) {
            // C# indexes must start with "from" for query syntax or
            // "docs" for method syntax
            if (reduce == null || StringUtils.isBlank(reduce)){
                return IndexType.MAP;
            }
            return IndexType.MAP_REDUCE;
        }

        if (StringUtils.isBlank(reduce)) {
            return IndexType.JAVA_SCRIPT_MAP;
        }

        return IndexType.JAVA_SCRIPT_MAP_REDUCE;
    }

    public static IndexSourceType detectStaticIndexSourceType(String map) {

        if (StringUtils.isBlank(map)) {
            throw new IllegalArgumentException("Value cannot be null or whitespace.");
        }

        map = stripComments(map);
        map = unifyWhiteSpace(map);

        // detect first supported syntax: timeseries.Companies.HeartRate.Where
        String mapLower = map.toLowerCase();
        if (mapLower.startsWith("timeseries")) {
            return IndexSourceType.TIME_SERIES;
        }

        if (mapLower.startsWith("counters")) {
            return IndexSourceType.COUNTERS;
        }

        if (map.startsWith("from")) {
            // detect `from ts in timeseries` or `from ts in timeseries.Users.HeartRate`

            String[] tokens = Arrays.stream(mapLower.split(" ", 4))
                    .filter(StringUtils::isNotEmpty)
                    .toArray(String[]::new);

            if (tokens.length >= 4 && "in".equalsIgnoreCase(tokens[2])) {
                if (tokens[3].startsWith("timeseries")) {
                    return IndexSourceType.TIME_SERIES;
                }
                if (tokens[3].startsWith("counters")) {
                    return IndexSourceType.COUNTERS;
                }
            }
        }

        // fallback to documents based index
        return IndexSourceType.DOCUMENTS;
    }

    private static String stripComments(String input) {
        return input
                .replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)","")
                .trim();
    }

    private static String unifyWhiteSpace(String input) {
        return input.replaceAll("\\s+", " ");
    }
}
