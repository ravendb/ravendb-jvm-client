package net.ravendb.client.extensions;

public class StringExtensions {

    public static String toWebSocketPath(String path) {
        return path.replaceAll("http://", "ws://")
                .replaceAll("https://", "wss://");
    }
}
