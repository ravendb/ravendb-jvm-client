package net.ravendb.client.extensions;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

public class StringExtensions {




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
