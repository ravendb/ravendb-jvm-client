package net.ravendb.client.test.suggestions;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexFieldOptions;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.queries.suggestions.StringDistanceTypes;
import net.ravendb.client.documents.queries.suggestions.SuggestionOptions;
import net.ravendb.client.documents.queries.suggestions.SuggestionResult;
import net.ravendb.client.documents.queries.suggestions.SuggestionSortMode;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.test.client.indexing.IndexesFromClientTest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SuggestionsTest extends RemoteTestBase {

    public static class User {
        private String id;
        private String name;
        private String email;

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }


    public void setup(IDocumentStore store) {

        IndexDefinition indexDefinition = new IndexDefinition();
        indexDefinition.setName("test");
        indexDefinition.setMaps(Collections.singleton("from doc in docs.Users select new { doc.name }"));
        IndexFieldOptions indexFieldOptions = new IndexFieldOptions();
        indexFieldOptions.setSuggestions(true);
        indexDefinition.setFields(Collections.singletonMap("name", indexFieldOptions));

        store.maintenance().send(new PutIndexesOperation(indexDefinition));

        try (IDocumentSession session = store.openSession()) {
            User user1 = new User();
            user1.setName("Ayende");

            User user2 = new User();
            user2.setName("Oren");

            User user3 = new User();
            user3.setName("John Steinbeck");

            session.store(user1);
            session.store(user2);
            session.store(user3);
            session.saveChanges();
        }

        waitForIndexing(store);
    }

    @Test
    public void exactMatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession session = store.openSession()) {
                SuggestionOptions options = new SuggestionOptions();
                options.setPageSize(10);

                Map<String, SuggestionResult> suggestionQueryResult = session.query(User.class, Query.index("test"))
                        .suggestUsing(x -> x.byField("name", "Oren").withOptions(options))
                        .execute();

                assertThat(suggestionQueryResult.get("name").getSuggestions())
                        .hasSize(0);
            }
        }
    }

    @Test
    public void usingLinq() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession s = store.openSession()) {
                Map<String, SuggestionResult> suggestionQueryResult = s.query(User.class, Query.index("test"))
                        .suggestUsing(x -> x.byField("name", "Owen"))
                        .execute();

                assertThat(suggestionQueryResult.get("name").getSuggestions())
                        .hasSize(1);

                assertThat(suggestionQueryResult.get("name").getSuggestions().get(0))
                        .isEqualTo("oren");
            }
        }
    }

    @Test
    public void usingLinq_WithOptions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession s = store.openSession()) {

                SuggestionOptions options = new SuggestionOptions();
                options.setAccuracy(0.4f);

                Map<String, SuggestionResult> suggestionQueryResult = s.query(User.class, Query.index("test"))
                        .suggestUsing(x -> x.byField("name", "Orin").withOptions(options))
                        .execute();

                assertThat(suggestionQueryResult.get("name").getSuggestions())
                        .hasSize(1);

                assertThat(suggestionQueryResult.get("name").getSuggestions().get(0))
                        .isEqualTo("oren");
            }
        }
    }

    @Test
    public void usingLinq_Multiple_words() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession s = store.openSession()) {

                SuggestionOptions options = new SuggestionOptions();
                options.setAccuracy(0.4f);
                options.setDistance(StringDistanceTypes.LEVENSHTEIN);

                Map<String, SuggestionResult> suggestionQueryResult = s.query(User.class, Query.index("test"))
                        .suggestUsing(x -> x.byField("name", "John Steinback").withOptions(options))
                        .execute();

                assertThat(suggestionQueryResult.get("name").getSuggestions())
                        .hasSize(1);

                assertThat(suggestionQueryResult.get("name").getSuggestions().get(0))
                        .isEqualTo("john steinbeck");
            }
        }
    }

    @Test
    public void withTypo() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession s = store.openSession()) {

                SuggestionOptions options = new SuggestionOptions();
                options.setAccuracy(0.2f);
                options.setPageSize(10);
                options.setDistance(StringDistanceTypes.LEVENSHTEIN);

                Map<String, SuggestionResult> suggestionQueryResult = s.query(User.class, Query.index("test"))
                        .suggestUsing(x -> x.byField("name", "Oern").withOptions(options))
                        .execute();

                assertThat(suggestionQueryResult.get("name").getSuggestions())
                        .hasSize(1);

                assertThat(suggestionQueryResult.get("name").getSuggestions().get(0))
                        .isEqualTo("oren");
            }
        }
    }

    @Test
    public void canGetSuggestions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.Users_ByName().execute(store);

            try (IDocumentSession s = store.openSession()) {
                User user1 = new User();
                user1.setName("John Smith");
                s.store(user1, "users/1");

                User user2 = new User();
                user2.setName("Jack Johnson");
                s.store(user2, "users/2");

                User user3 = new User();
                user3.setName("Robery Jones");
                s.store(user3, "users/3");

                User user4 = new User();
                user4.setName("David Jones");
                s.store(user4, "users/4");

                s.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                SuggestionOptions options = new SuggestionOptions();
                options.setAccuracy(0.4f);
                options.setPageSize(5);
                options.setDistance(StringDistanceTypes.JARO_WINKLER);
                options.setSortMode(SuggestionSortMode.POPULARITY);

                Map<String, SuggestionResult> suggestions = session.query(User.class, IndexesFromClientTest.Users_ByName.class)
                        .suggestUsing(x -> x.byField("name", new String[]{"johne", "davi"}).withOptions(options))
                        .execute();

                assertThat(suggestions.get("name").getSuggestions())
                        .hasSize(5);

                assertThat(suggestions.get("name").getSuggestions())
                        .containsSequence("john", "jones", "johnson", "david", "jack");
            }
        }
    }
}
