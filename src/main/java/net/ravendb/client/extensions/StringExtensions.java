package net.ravendb.client.extensions;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

public class StringExtensions {

    public static String toWebSocketPath(String path) {
        return path.replaceAll("http://", "ws://")
                .replaceAll("https://", "wss://");
    }

    public static boolean isIdentifier(String token) {
        return isIdentifier(token, 0, token.length());
    }

    public static boolean isIdentifier(String token, int start, int length) {
        if (length == 0 || length > 256) {
            return false;
        }

        if (!Character.isLetter(token.charAt(start)) && token.charAt(start) != '_') {
            return false;
        }

        for (int i = 1; i < length; i++) {
            if (!Character.isLetterOrDigit(token.charAt(start + i)) && token.charAt(start + i) != '_') {
                return false;
            }
        }

        return true;
    }

    public static void escapeString(StringBuilder builder, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }

        escapeStringInternal(builder, value);
    }

    private static void escapeStringInternal(StringBuilder builder, String value) {
        String escaped = StringEscapeUtils.escapeEcmaScript(value);
        builder.append(escaped);
    }
}
