package net.ravendb.client.documents.indexes;

import org.apache.commons.lang3.StringUtils;

public class IndexDefinitionHelper {
    public static IndexType detectStaticIndexType(String map, String reduce) {

        if (map.isEmpty()) {
            throw new IllegalArgumentException("Index definitions contains no Maps");
        }

        map = map.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)","");
        map = map.trim();

        if (map.startsWith("from") || map.startsWith("docs")) {
            // C# indexes must start with "from" for query synatx or
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
}
