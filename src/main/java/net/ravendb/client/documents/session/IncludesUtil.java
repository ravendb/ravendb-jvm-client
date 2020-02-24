package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

public class IncludesUtil {

    @SuppressWarnings("UnnecessaryReturnStatement")
    public static void include(ObjectNode document, String include, Consumer<String> loadId) {
        if (StringUtils.isEmpty(include) || document == null){
            return;
        }

        //TBD:
    }

    /* TBD
     public static void Include(BlittableJsonReaderObject document, string include, Action<string> loadId)
        {
            if (string.IsNullOrEmpty(include) || document == null)
                return;
            var path = GetIncludePath(include, out var isPrefix);

            foreach (var token in document.SelectTokenWithRavenSyntaxReturningFlatStructure(path.Path))
            {
                ExecuteInternal(token.Item1, path.Addition, (value, addition) =>
                {
                    value = addition != null
                        ? (isPrefix ? addition + value : string.Format(addition, value))
                        : value;

                    loadId(value);
                });
            }
        }
     */

    public static boolean requiresQuotes(String include, Reference<String> escapedInclude) {
        for (int i = 0; i < include.length(); i++) {
            char ch = include.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '.') {
                escapedInclude.value = include.replaceAll("'", "\\'");
                return true;
            }
        }

        escapedInclude.value = null;
        return false;
    }
}
