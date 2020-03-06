package net.ravendb.client.test.issues;

import com.google.common.collect.Sets;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexFieldOptions;
import net.ravendb.client.documents.indexes.PutIndexResult;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.queries.suggestions.SuggestionResult;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_9584Test extends RemoteTestBase {

    private void setup(IDocumentStore store) {

        IndexDefinition indexDefinition = new IndexDefinition();
        indexDefinition.setName("test");
        indexDefinition.setMaps(Sets.newHashSet("from doc in docs.Users select new { doc.name, doc.company }"));

        IndexFieldOptions nameIndexFieldOptions = new IndexFieldOptions();
        nameIndexFieldOptions.setSuggestions(true);

        IndexFieldOptions companyIndexFieldOptions = new IndexFieldOptions();
        companyIndexFieldOptions.setSuggestions(true);

        Map<String, IndexFieldOptions> fields = new HashMap<>();
        fields.put("name", nameIndexFieldOptions);
        fields.put("company", companyIndexFieldOptions);

        indexDefinition.setFields(fields);

        PutIndexesOperation putIndexesOperation = new PutIndexesOperation(indexDefinition);

        PutIndexResult[] results = store.maintenance().send(putIndexesOperation);
        assertThat(results)
                .hasSize(1);

        assertThat(results[0].getIndex())
                .isEqualTo(indexDefinition.getName());

        try (IDocumentSession session = store.openSession()) {
            User ayende = new User();
            ayende.setName("Ayende");
            ayende.setCompany("Hibernating");

            User oren = new User();
            oren.setName("Oren");
            oren.setCompany("HR");

            User john = new User();
            john.setName("John Steinbeck");
            john.setCompany("Unknown");

            session.store(ayende);
            session.store(oren);
            session.store(john);
            session.saveChanges();

            waitForIndexing(store);
        }
    }
    
    @Test
    public void canChainSuggestions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession s = store.openSession()) {
                Map<String, SuggestionResult> suggestionQueryResult = s.query(User.class, Query.index("test"))
                        .suggestUsing(x -> x.byField("name", "Owen"))
                        .andSuggestUsing(x -> x.byField("company", "Hiberanting"))
                        .execute();

                assertThat(suggestionQueryResult.get("name").getSuggestions())
                        .hasSize(1);
                assertThat(suggestionQueryResult.get("name").getSuggestions().get(0))
                        .isEqualTo("oren");

                assertThat(suggestionQueryResult.get("company").getSuggestions())
                        .hasSize(1);
                assertThat(suggestionQueryResult.get("company").getSuggestions().get(0))
                        .isEqualTo("hibernating");
            }
        }
    }

    @Test
    public void canUseAliasInSuggestions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession s = store.openSession()) {
                Map<String, SuggestionResult> suggestionQueryResult = s.query(User.class, Query.index("test"))
                        .suggestUsing(x -> x.byField("name", "Owen").withDisplayName("newName"))
                        .execute();

                assertThat(suggestionQueryResult.get("newName").getSuggestions())
                        .hasSize(1);
                assertThat(suggestionQueryResult.get("newName").getSuggestions().get(0))
                        .isEqualTo("oren");
            }
        }
    }

    @Test
    public void canUseSuggestionsWithAutoIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession s = store.openSession()) {
                Map<String, SuggestionResult> suggestionQueryResult = s.query(User.class)
                        .suggestUsing(x -> x.byField("name", "Owen").withDisplayName("newName"))
                        .execute();

                assertThat(suggestionQueryResult.get("newName").getSuggestions())
                        .hasSize(1);
                assertThat(suggestionQueryResult.get("newName").getSuggestions().get(0))
                        .isEqualTo("oren");
            }
        }
    }

    public static class User {
        private String id;
        private String name;
        private String company;
        private String title;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
