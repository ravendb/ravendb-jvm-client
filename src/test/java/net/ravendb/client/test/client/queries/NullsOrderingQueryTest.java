package net.ravendb.client.test.client.queries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.NullsOrdering;
import net.ravendb.client.documents.session.OrderingType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NullsOrderingQueryTest extends RemoteTestBase {

    public static class Item {
        private String name;
        private Long age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getAge() {
            return age;
        }

        public void setAge(Long age) {
            this.age = age;
        }
    }

    @Test
    public void orderByEmitsNullsFirst() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Item> query = session.advanced().documentQuery(Item.class)
                        .orderBy("name", NullsOrdering.FIRST, OrderingType.STRING);

                assertThat(query.toString())
                        .contains("order by name nulls first");
            }
        }
    }

    @Test
    public void orderByDescendingEmitsNullsLast() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Item> query = session.advanced().documentQuery(Item.class)
                        .orderByDescending("name", NullsOrdering.LAST, OrderingType.STRING);

                assertThat(query.toString())
                        .contains("order by name desc nulls last");
            }
        }
    }

    @Test
    public void orderByWithOrderingTypeEmitsNullsClause() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Item> query = session.advanced().documentQuery(Item.class)
                        .orderBy("age", NullsOrdering.LAST, OrderingType.LONG);

                assertThat(query.toString())
                        .contains("order by age as long nulls last");
            }
        }
    }

    @Test
    public void defaultNullsOrderingEmitsNoNullsClause() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Item> query = session.advanced().documentQuery(Item.class)
                        .orderBy("name", NullsOrdering.DEFAULT, OrderingType.STRING);

                assertThat(query.toString())
                        .doesNotContain("nulls");
            }
        }
    }

    @Test
    public void orderByDistanceEmitsNullsClause() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Item> query = session.advanced().documentQuery(Item.class)
                        .orderByDistance("location", 10.0, 20.0, NullsOrdering.FIRST);

                assertThat(query.toString())
                        .contains("nulls first");
            }
        }
    }
}
