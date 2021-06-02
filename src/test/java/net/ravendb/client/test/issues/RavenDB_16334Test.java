package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldStorage;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_16334Test extends RemoteTestBase {

    @Test
    public void canWaitForIndexesWithLoadAfterSaveChangesAllIndexes() throws Exception {
        canWaitForIndexesWithLoadAfterSaveChangesInternal(true);
    }

    @Test
    public void canWaitForIndexesWithLoadAfterSaveChangesSingleIndex() throws Exception {
        canWaitForIndexesWithLoadAfterSaveChangesInternal(false);
    }

    private void canWaitForIndexesWithLoadAfterSaveChangesInternal(boolean allIndexes) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            new MyIndex().execute(store);

            try (IDocumentSession session = store.openSession()) {
                MainDocument mainDocument = new MainDocument();
                mainDocument.setName("A");
                mainDocument.setId("main/A");
                session.store(mainDocument);

                RelatedDocument relatedDocument = new RelatedDocument();
                relatedDocument.setName("A");
                relatedDocument.setValue(21.5);
                relatedDocument.setId("related/A");
                session.store(relatedDocument);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                MyIndex.Result result = session.query(MyIndex.Result.class, MyIndex.class)
                        .selectFields(MyIndex.Result.class)
                        .single();
                assertThat(result.value)
                        .isEqualTo(21.5);
            }

            // act
            try (IDocumentSession session = store.openSession()) {
                session.advanced().waitForIndexesAfterSaveChanges(builder -> {
                    builder
                            .withTimeout(Duration.ofSeconds(15))
                            .throwOnTimeout(true)
                            .waitForIndexes(allIndexes ? null : new String[] { "MyIndex" });
                });

                RelatedDocument related = session.load(RelatedDocument.class, "related/A");
                related.value = 42;
                session.saveChanges();
            }

            // assert
            try (IDocumentSession session = store.openSession()) {
                MyIndex.Result result = session.query(MyIndex.Result.class, MyIndex.class)
                        .selectFields(MyIndex.Result.class)
                        .single();

                assertThat(result.getValue())
                        .isEqualTo(42);
            }
        }
    }

    public static class MainDocument {
        private String name;
        private String id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class RelatedDocument {
        private String name;
        private double value;
        private String id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class MyIndex extends AbstractIndexCreationTask {
        public static class Result {
            private String name;
            private Double value;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public Double getValue() {
                return value;
            }

            public void setValue(Double value) {
                this.value = value;
            }
        }

        public MyIndex() {

            map = "docs.MainDocuments.Select(mainDocument => new {" +
                    "    mainDocument = mainDocument," +
                    "    related = this.LoadDocument(String.Format(\"related/{0}\", mainDocument.name), \"RelatedDocuments\")" +
                    "}).Select(this0 => new {" +
                    "    name = this0.mainDocument.name,\n" +
                    "    value = this0.related != null ? ((decimal ? ) this0.related.value) : ((decimal ? ) null)\n" +
                    "})";

            storeAllFields(FieldStorage.YES);
        }
    }
}
