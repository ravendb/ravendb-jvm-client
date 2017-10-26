package net.ravendb.client.primitives;


public class Lang {
    @SuppressWarnings("unchecked")
    public static <T> T coalesce(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
