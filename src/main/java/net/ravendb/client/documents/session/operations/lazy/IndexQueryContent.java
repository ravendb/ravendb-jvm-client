package net.ravendb.client.documents.session.operations.lazy;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.extensions.JsonExtensions;

import java.io.IOException;

public class IndexQueryContent implements GetRequest.IContent {
    private final DocumentConventions _conventions;
    private final IndexQuery _query;

    public IndexQueryContent(DocumentConventions conventions, IndexQuery query) {
        _conventions = conventions;
        _query = query;
    }

    @Override
    public void writeContent(JsonGenerator generator) throws IOException {
        JsonExtensions.writeIndexQuery(generator, _conventions, _query);
    }
}
