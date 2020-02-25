package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_9889Test extends RemoteTestBase {

    public static class Item {
        private String id;
        private boolean before;
        private boolean after;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isBefore() {
            return before;
        }

        public void setBefore(boolean before) {
            this.before = before;
        }

        public boolean isAfter() {
            return after;
        }

        public void setAfter(boolean after) {
            this.after = after;
        }
    }

    public static class ProjectedItem {
        private boolean before;
        private boolean after;

        public boolean isBefore() {
            return before;
        }

        public void setBefore(boolean before) {
            this.before = before;
        }

        public boolean isAfter() {
            return after;
        }

        public void setAfter(boolean after) {
            this.after = after;
        }
    }

    @Test
    public void canUseToDocumentConversionEvents() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            store.addBeforeConversionToDocumentListener((sender, event) -> {
                if (event.getEntity() instanceof Item) {
                    Item item = (Item) event.getEntity();
                    item.setBefore(true);
                }
            });

            store.addAfterConversionToDocumentListener((sender, event) -> {
                if (event.getEntity() instanceof Item) {
                    Item item = (Item) event.getEntity();
                    ObjectNode document = event.getDocument().value.deepCopy();
                    document.put("after", true);
                    event.getDocument().value = document;

                    item.setAfter(true);
                }
            });

            try (IDocumentSession session = store.openSession()) {
                session.store(new Item(), "items/1");
                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                Item item = session.load(Item.class, "items/1");

                assertThat(item)
                        .isNotNull();

                assertThat(item.isBefore())
                        .isTrue();
                assertThat(item.isAfter())
                        .isTrue();
            }
        }
    }

    @Test
    public void canUseToEntityConversionEvents() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            store.addBeforeConversionToEntityListener((sender, event) -> {
                ObjectNode document = event.getDocument().value.deepCopy();

                document.put("before", true);
                event.getDocument().value = document;
            });

            store.addAfterConversionToEntityListener((sender, event) -> {
                if (event.getEntity() instanceof Item) {
                    Item item = (Item) event.getEntity();
                    item.setAfter(true);
                }

                if (event.getEntity() instanceof ProjectedItem) {
                    ProjectedItem projectedItem = (ProjectedItem) event.getEntity();
                    projectedItem.setAfter(true);
                }
            });

            try (IDocumentSession session = store.openSession()) {
                session.store(new Item(), "items/1");
                session.store(new Item(), "items/2");
                session.saveChanges();
            }

            // load
            try (IDocumentSession session = store.openSession()) {
                Item item = session.load(Item.class, "items/1");

                assertThat(item)
                        .isNotNull();
                assertThat(item.isBefore())
                        .isTrue();
                assertThat(item.isAfter())
                        .isTrue();
            }

            // queries
            try (IDocumentSession session = store.openSession()) {
                List<Item> items =
                        session.query(Item.class).toList();

                assertThat(items)
                        .hasSize(2);

                for (Item item : items) {
                    assertThat(item.isBefore())
                            .isTrue();
                    assertThat(item.isAfter())
                            .isTrue();
                }
            }

            // projections in queries
            try (IDocumentSession session = store.openSession()) {
                List<ProjectedItem> items = session
                        .query(Item.class)
                        .selectFields(ProjectedItem.class)
                        .toList();

                assertThat(items)
                        .hasSize(2);

                for (ProjectedItem item : items) {
                    assertThat(item.isBefore())
                            .isTrue();
                    assertThat(item.isAfter())
                            .isTrue();
                }
            }
        }
    }
}