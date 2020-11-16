package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15826Test extends RemoteTestBase {

    @Test
    public void canIncludeLazyLoadITemThatIsAlreadyOnSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new Item(), "items/a");
                session.store(new Item(), "items/b");
                Item itemC = new Item();
                itemC.setRefs(new String[]{  "items/a", "items/b" });
                session.store(itemC, "items/c");
                Item itemD = new Item();
                itemD.setRefs(new String[]{ "items/a" });
                session.store(itemD, "items/d");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.include("refs").load(Item.class, "items/d"); // include, some loaded
                Item a = session.load(Item.class, "items/c");// include, some loaded
                Lazy<Map<String, Item>> items = session.advanced().lazily().load(Item.class, Arrays.asList(a.refs));
                session.advanced().eagerly().executeAllPendingLazyOperations();
                Map<String, Item> itemsMap = items.getValue();
                assertThat(a.refs.length)
                        .isEqualTo(itemsMap.size());
                assertThat(itemsMap.values().stream().filter(Objects::isNull).toArray())
                        .isEmpty();
            }
        }
    }

    public static class Item {
        private String[] refs;

        public String[] getRefs() {
            return refs;
        }

        public void setRefs(String[] refs) {
            this.refs = refs;
        }
    }
}
