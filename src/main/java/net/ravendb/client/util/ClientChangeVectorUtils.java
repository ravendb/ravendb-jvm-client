package net.ravendb.client.util;

public final class ClientChangeVectorUtils {

    private ClientChangeVectorUtils() {
        // private constructor to prevent instantiation
    }

    public static long getEtagById(String changeVector, String id) {
        if (changeVector == null) {
            return 0L;
        }

        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        int index = changeVector.indexOf("-" + id);
        if (index == -1) {
            return 0L;
        }

        int end = index - 1;
        int start = changeVector.lastIndexOf(":", end) + 1;

        return Long.parseLong(changeVector.substring(start, end + 1));
    }
}

