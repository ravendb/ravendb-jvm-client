package net.ravendb.client.test.suggestions;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexFieldOptions;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.queries.suggestions.SuggestionResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SuggestionsLazyTest extends RemoteTestBase {

    @Test
    public void usingLinq() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            IndexDefinition indexDefinition = new IndexDefinition();
            indexDefinition.setName("Test");
            indexDefinition.setMaps(Collections.singleton("from doc in docs.Users select new { doc.name }"));
            IndexFieldOptions indexFieldOptions = new IndexFieldOptions();
            indexFieldOptions.setSuggestions(true);
            indexDefinition.setFields(Collections.singletonMap("name", indexFieldOptions));

            store.maintenance().send(new PutIndexesOperation(indexDefinition));

            try (IDocumentSession s = store.openSession()) {
                User user1 = new User();
                user1.setName("Ayende");
                s.store(user1);

                User user2 = new User();
                user2.setName("Oren");
                s.store(user2);

                s.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession s = store.openSession()) {
                int oldRequests = s.advanced().getNumberOfRequests();

                Lazy<Map<String, SuggestionResult>> suggestionQueryResult = s.query(User.class, Query.index("test"))
                        .suggestUsing(x -> x.byField("name", "Owen"))
                        .executeLazy();

                assertThat(s.advanced().getNumberOfRequests())
                        .isEqualTo(oldRequests);

                assertThat(suggestionQueryResult.getValue().get("name").getSuggestions())
                        .hasSize(1);

                assertThat(suggestionQueryResult.getValue().get("name").getSuggestions().get(0))
                        .isEqualTo("oren");

                assertThat(s.advanced().getNumberOfRequests())
                        .isEqualTo(oldRequests + 1);
            }
        }
    }
}
