package net.ravendb.client.documents.indexes;

public class IndexTypeExtensions {

    private IndexTypeExtensions() {
    }

    public static boolean isMap(IndexType type) {
        return type == IndexType.MAP || type == IndexType.AUTO_MAP || type == IndexType.JAVA_SCRIPT_MAP;
    }

    public static boolean isMapReduce(IndexType type) {
        return type == IndexType.MAP_REDUCE || type == IndexType.AUTO_MAP_REDUCE || type == IndexType.JAVA_SCRIPT_MAP_REDUCE;
    }

    public static boolean isAuto(IndexType type) {
        return type == IndexType.AUTO_MAP || type == IndexType.AUTO_MAP_REDUCE;
    }

    public static boolean isStale(IndexType type) {
        return type == IndexType.MAP || type == IndexType.MAP_REDUCE || type == IndexType.JAVA_SCRIPT_MAP || type == IndexType.JAVA_SCRIPT_MAP_REDUCE;
    }

    public static boolean isJavaScript(IndexType type) {
        return type == IndexType.JAVA_SCRIPT_MAP || type == IndexType.JAVA_SCRIPT_MAP_REDUCE;
    }
}
