package net.ravendb.client.documents.queries;

import net.ravendb.client.Constants;
import org.apache.commons.lang3.StringUtils;

public class QueryFieldUtil {
    public static String escapeIfNecessary(String name) {
        return escapeIfNecessary(name, false);
    }

    private static boolean shouldEscape(String s, boolean isPath) {
        boolean escape = false;
        boolean insideEscaped = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '\'' || c == '"') {
                insideEscaped = !insideEscaped;
                continue;
            }

            if (i == 0) {
                if (!Character.isLetter(c) && c != '_' && c != '@' && !insideEscaped) {
                    escape = true;
                    break;
                }
            } else {
                if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '@' && c != '.' && c != '[' && c != ']' && !insideEscaped) {
                    escape = true;
                    break;
                }
                if (isPath && c == '.' && !insideEscaped) {
                    escape = true;
                    break;
                }
            }
        }

        escape |= insideEscaped;
        return escape;
    }

    @SuppressWarnings("ConstantConditions")
    public static String escapeIfNecessary(String name, boolean isPath) {
        if (StringUtils.isEmpty(name) ||
                Constants.Documents.Indexing.Fields.DOCUMENT_ID_FIELD_NAME.equals(name) ||
                Constants.Documents.Indexing.Fields.REDUCE_KEY_HASH_FIELD_NAME.equals(name) ||
                Constants.Documents.Indexing.Fields.REDUCE_KEY_KEY_VALUE_FIELD_NAME.equals(name) ||
                Constants.Documents.Indexing.Fields.VALUE_FIELD_NAME.equals(name) ||
                Constants.Documents.Indexing.Fields.SPATIAL_SHAPE_FIELD_NAME.equals(name)) {
            return name;
        }

        if (!shouldEscape(name, isPath)) {
            return name;
        }

        StringBuilder sb = new StringBuilder(name);
        boolean needEndQuote = false;
        int lastTermStart = 0;

        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (i == 0 && !Character.isLetter(c) && c != '_' && c != '@') {
                sb.insert(lastTermStart, '\'');
                needEndQuote = true;
                continue;
            }

            if (isPath && c == '.') {
                if (needEndQuote) {
                    needEndQuote = false;
                    sb.insert(i, '\'');
                    i++;
                }

                lastTermStart = i + 1;
                continue;
            }

            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '@' && c != '.' && c != '[' && c != ']' && !needEndQuote) {
                sb.insert(lastTermStart, '\'');
                needEndQuote = true;
                continue;
            }
        }

        if (needEndQuote) {
            sb.append('\'');
        }

        return sb.toString();
    }
}
