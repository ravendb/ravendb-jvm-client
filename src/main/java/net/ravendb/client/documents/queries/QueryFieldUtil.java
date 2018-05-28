package net.ravendb.client.documents.queries;

import net.ravendb.client.Constants;
import org.apache.commons.lang3.StringUtils;

public class QueryFieldUtil {
    @SuppressWarnings("ConstantConditions")
    public static String escapeIfNecessary(String name) {
        if (StringUtils.isEmpty(name) ||
                Constants.Documents.Indexing.Fields.DOCUMENT_ID_FIELD_NAME.equals(name) ||
                Constants.Documents.Indexing.Fields.REDUCE_KEY_HASH_FIELD_NAME.equals(name) ||
                Constants.Documents.Indexing.Fields.REDUCE_KEY_KEY_VALUE_FIELD_NAME.equals(name) ||
                Constants.Documents.Indexing.Fields.SPATIAL_SHAPE_FIELD_NAME.equals(name)) {
            return name;
        }

        boolean escape = false;
        boolean insideEscaped = false;

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

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
            }
        }

        if (escape || insideEscaped) {
            return "'" + name + "'";
        }

        return name;
    }
}
