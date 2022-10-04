package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.DocumentStore;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_18643Test extends RemoteTestBase {


    @Test
    public void canGetProgressOfBulkInsert() throws Exception {
        List<String> lastInsertedDocId = new ArrayList<>();

        try (DocumentStore store = getDocumentStore()) {
            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {

                bulkInsert.addOnProgress((sender, event) -> {
                    lastInsertedDocId.add(event.getProgress().getLastProcessedId());
                    assertThat(event.getProgress().getLastProcessedId())
                            .isNotEmpty();
                });

                int i = 0;

                while (lastInsertedDocId.isEmpty()) {
                    ExampleItem exampleItem = new ExampleItem();
                    exampleItem.setName("ExampleItem/" + i++);
                    bulkInsert.store(exampleItem);
                    Thread.sleep(200);
                }

                assertThat(lastInsertedDocId)
                        .isNotEmpty();
            }
        }
    }

    public static class ExampleItem {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
