package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_17420Test extends RemoteTestBase {

    @Test
    public void can_use_boost_on_in_query() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                Item item = new Item();
                item.setName("ET");
                session.store(item);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Item first = session.advanced().documentQuery(Item.class)
                        .whereIn("name", Arrays.asList("ET", "Alien"))
                        .boost(0)
                        .first();

                assertThat(session.advanced().getMetadataFor(first).getDouble("@index-score"))
                        .isZero();
            }
        }
    }

    public static class Item {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
