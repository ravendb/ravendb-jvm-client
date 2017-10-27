package net.ravendb.client.test.client.documents;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.entities.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadTest extends RemoteTestBase {

    private static class Foo {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static class Bar {
        private String fooId;
        private List<String> fooIDs;
        private String name;

        public String getFooId() {
            return fooId;
        }

        public void setFooId(String fooId) {
            this.fooId = fooId;
        }

        public List<String> getFooIDs() {
            return fooIDs;
        }

        public void setFooIDs(List<String> fooIDs) {
            this.fooIDs = fooIDs;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void loadWithIncludes() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            String barId;

            try (IDocumentSession session = store.openSession()) {
                Foo foo = new Foo();
                foo.setName("Beginning");
                session.store(foo);

                String fooId = barId = session.advanced().getDocumentId(foo);
                Bar bar = new Bar();
                bar.setName("End");
                bar.setFooId(fooId);

                session.store(bar);

                barId = session.advanced().getDocumentId(bar);
                session.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                Map<String, Bar> bar = newSession
                        .include("fooId")
                        .load(Bar.class, new String[]{barId});

                assertThat(bar)
                        .isNotNull();

                assertThat(bar)
                        .hasSize(1);

                assertThat(bar.get(barId))
                        .isNotNull();

                int numOfRequests = newSession.advanced().getNumberOfRequests();

                Foo foo = newSession.load(Foo.class, bar.get(barId).getFooId());

                assertThat(foo)
                        .isNotNull();

                assertThat(foo.getName())
                        .isEqualTo("Beginning");

                assertThat(newSession.advanced().getNumberOfRequests())
                        .isEqualTo(numOfRequests);
            }
        }
    }


    @Test
    public void loadWithIncludesAndMissingDocument() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            String barId;

            try (IDocumentSession session = store.openSession()) {
                Bar bar = new Bar();
                bar.setName("End");
                bar.setFooId("somefoo/1");

                session.store(bar);
                barId = session.advanced().getDocumentId(bar);
                session.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                Map<String, Bar> bar = newSession.include("fooId")
                        .load(Bar.class, new String[] { barId });

                assertThat(bar)
                        .isNotNull()
                        .hasSize(1);

                assertThat(bar.get(barId))
                        .isNotNull();

                int numOfRequests = newSession.advanced().getNumberOfRequests();

                Foo foo = newSession.load(Foo.class, bar.get(barId).getFooId());

                assertThat(foo)
                        .isNull();

                assertThat(newSession.advanced().getNumberOfRequests())
                        .isEqualTo(numOfRequests);
            }
        }
    }
}
