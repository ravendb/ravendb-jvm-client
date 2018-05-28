package net.ravendb.client.documents.queries.moreLikeThis;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.documents.session.tokens.MoreLikeThisToken;
import net.ravendb.client.extensions.JsonExtensions;

import java.util.function.Function;

public class MoreLikeThisScope implements AutoCloseable {

    private final MoreLikeThisToken _token;
    private final Function<Object, String> _addQueryParameter;
    private final Runnable _onDispose;

    public MoreLikeThisScope(MoreLikeThisToken token, Function<Object, String> addQueryParameter, Runnable onDispose) {
        _token = token;
        _addQueryParameter = addQueryParameter;
        _onDispose = onDispose;
    }

    @Override
    public void close() {
        if (_onDispose != null) {
            _onDispose.run();
        }
    }

    public void withOptions(MoreLikeThisOptions options) {
        if (options == null) {
            return;
        }

        // force using *non* entity serializer here:
        JsonNode optionsAsJson = JsonExtensions.getDefaultMapper().valueToTree(options);
        _token.optionsParameterName = _addQueryParameter.apply(optionsAsJson);
    }

    public void withDocument(String document) {
        _token.documentParameterName = _addQueryParameter.apply(document);
    }
}
