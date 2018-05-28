package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
}
