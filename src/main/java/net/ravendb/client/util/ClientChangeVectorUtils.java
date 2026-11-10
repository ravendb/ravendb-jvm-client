package net.ravendb.client.util;

public final class ClientChangeVectorUtils {

    public static final char SEPARATOR = '|';

    private ClientChangeVectorUtils() {
    }

    public static long getEtagById(String changeVector, String id) {
        if (changeVector == null) {
            return 0;
        }

        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        if (changeVector.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException("Change vector contains '" + SEPARATOR + "', which is not supported for this operation.");
        }

        int index = changeVector.indexOf("-" + id);
        if (index == -1) {
            return 0;
        }

        int end = index - 1;
        int start = changeVector.lastIndexOf(":", end) + 1;

        return Long.parseLong(changeVector.substring(start, end + 1));
    }
}
