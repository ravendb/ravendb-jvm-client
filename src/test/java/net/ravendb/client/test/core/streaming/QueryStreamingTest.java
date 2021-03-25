package net.ravendb.client.test.core.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.indexes.FieldStorage;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IRawDocumentQuery;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.Reference;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryStreamingTest extends RemoteTestBase {

    @Test
    public void canStreamQueryResults() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Users_ByName().execute(store);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 200; i++) {
                    session.store(new User());
                }

                session.saveChanges();
            }

            waitForIndexing(store);

            int count = 0;

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<User> query = session.query(User.class, Users_ByName.class);
                try (CloseableIterator<StreamResult<User>> stream = session.advanced().stream(query)) {
                    while (stream.hasNext()) {
                        StreamResult<User> user = stream.next();
                        count++;

                        assertThat(user)
                                .isNotNull();
                    }
                }
            }

            assertThat(count)
                    .isEqualTo(200);
        }
    }

    @Test
    public void canStreamQueryResultsWithQueryStatistics() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Users_ByName().execute(store);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 100; i++) {
                    session.store(new User());
                }
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<User> query = session.query(User.class, Users_ByName.class);

                Reference<StreamQueryStatistics> statsRef = new Reference<>();
                try (CloseableIterator<StreamResult<User>> reader = session.advanced().stream(query, statsRef)) {
                    while (reader.hasNext()) {
                        StreamResult<User> user = reader.next();
                        assertThat(user)
                                .isNotNull();
                    }

                    assertThat(statsRef.value.getIndexName())
                            .isEqualTo("Users/ByName");

                    assertThat(statsRef.value.getTotalResults())
                            .isEqualTo(100);

                    assertThat(statsRef.value.getIndexTimestamp())
                            .isInSameYearAs(new Date());
                }
            }
        }
    }

    @Test
    public void canStreamRawQueryResults() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Users_ByName().execute(store);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 200; i++) {
                    session.store(new User());
                }

                session.saveChanges();
            }

            waitForIndexing(store);

            int count = 0;

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<User> query = session.advanced().rawQuery(User.class, "from index '" + new Users_ByName().getIndexName() + "'");
                try (CloseableIterator<StreamResult<User>> stream = session.advanced().stream(query)) {
                    while (stream.hasNext()) {
                        StreamResult<User> user = stream.next();
                        count++;

                        assertThat(user)
                                .isNotNull();
                    }
                }
            }

            assertThat(count)
                    .isEqualTo(200);
        }
    }

    @Test
    public void canStreamRawQueryResultsWithQueryStatistics() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Users_ByName().execute(store);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 100; i++) {
                    session.store(new User());
                }
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<User> query = session.advanced().rawQuery(User.class, "from index '" + new Users_ByName().getIndexName() + "'");

                Reference<StreamQueryStatistics> statsRef = new Reference<>();
                try (CloseableIterator<StreamResult<User>> reader = session.advanced().stream(query, statsRef)) {
                    while (reader.hasNext()) {
                        StreamResult<User> user = reader.next();
                        assertThat(user)
                                .isNotNull();
                    }

                    assertThat(statsRef.value.getIndexName())
                            .isEqualTo("Users/ByName");

                    assertThat(statsRef.value.getTotalResults())
                            .isEqualTo(100);

                    assertThat(statsRef.value.getIndexTimestamp())
                            .isInSameYearAs(new Date());
                }
            }
        }
    }

    @Test
    public void canStreamRawQueryIntoStream() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Users_ByName().execute(store);

            try (IDocumentSession session = store.openSession()) {
                session.store(new User());
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<User> query = session.advanced().rawQuery(User.class, "from index '" + new Users_ByName().getIndexName() + "'");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                session.advanced().streamInto(query, baos);

                JsonNode queryResult = JsonExtensions.getDefaultMapper().readTree(baos.toByteArray());
                assertThat(queryResult)
                        .isInstanceOf(ObjectNode.class);

                assertThat(queryResult.get("Results").get(0))
                        .isNotNull();
            }
        }
    }

    @Test
    public void canStreamQueryIntoStream() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Users_ByName().execute(store);

            try (IDocumentSession session = store.openSession()) {
                session.store(new User());
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<User> query = session.query(User.class, Users_ByName.class);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                session.advanced().streamInto(query, baos);

                JsonNode queryResult = JsonExtensions.getDefaultMapper().readTree(baos.toByteArray());
                assertThat(queryResult)
                        .isInstanceOf(ObjectNode.class);

                assertThat(queryResult.get("Results").get(0))
                        .isNotNull();
            }
        }
    }

    public static class Users_ByName extends AbstractIndexCreationTask {
        public Users_ByName() {
            map = "from u in docs.Users select new { u.name, lastName = u.lastName.Boost(10) }";

            index("name", FieldIndexing.SEARCH);

            indexSuggestions.add("name");

            store("name", FieldStorage.YES);
        }
    }
}
