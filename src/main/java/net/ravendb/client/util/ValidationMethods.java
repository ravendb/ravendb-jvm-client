package net.ravendb.client.util;

public class ValidationMethods {
    public static  <T> void assertNotNullOrEmpty(T key, String keyName) {
        if (key == null) {
            throw new IllegalArgumentException(keyName);
        }

        if (key instanceof String && ((String) key).isEmpty()) {
            throw new IllegalArgumentException(keyName + " cannot be null or empty");
        }
    }
}
