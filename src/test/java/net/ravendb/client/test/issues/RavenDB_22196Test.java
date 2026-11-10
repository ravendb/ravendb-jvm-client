package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTaskBase;
import net.ravendb.client.documents.indexes.AbstractMultiMapIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldStorage;
import net.ravendb.client.documents.indexes.IndexStats;
import net.ravendb.client.documents.operations.indexes.DisableIndexOperation;
import net.ravendb.client.documents.operations.indexes.GetIndexStatisticsOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "6.0")
public class RavenDB_22196Test extends RemoteTestBase {

    // the turnOffDatabase variants of the C# test unload the database through server internals, so they are not ported

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void singleCollectionLoadTest(boolean disableIndex) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            arrange(store, new SingleCollectionLoad(), disableIndex);

            IndexStats indexStats = store.maintenance().send(new GetIndexStatisticsOperation("SingleCollectionLoad"));
            assertThat(indexStats)
                    .isNotNull();
            assertThat(indexStats.getReferencedCollections())
                    .containsExactly("Item2s");
            assertThat(indexStats.isInvalidIndex())
                    .isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void multipleCollectionLoadTest(boolean disableIndex) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            arrange(store, new MultipleCollectionLoad(), disableIndex);

            IndexStats indexStats = store.maintenance().send(new GetIndexStatisticsOperation("MultipleCollectionLoad"));
            assertThat(indexStats)
                    .isNotNull();
            assertThat(indexStats.getReferencedCollections())
                    .containsExactlyInAnyOrder("Item2s", "Item3s");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void multiMapLoadTest(boolean disableIndex) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            arrange(store, new MultiMapLoad(), disableIndex);

            IndexStats indexStats = store.maintenance().send(new GetIndexStatisticsOperation("MultiMapLoad"));
            assertThat(indexStats)
                    .isNotNull();
            assertThat(indexStats.getReferencedCollections())
                    .containsExactlyInAnyOrder("Item2s", "Item3s");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void multiMapLoadWithRepetitionTest(boolean disableIndex) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            arrange(store, new MultiMapLoadWithRepetition(), disableIndex);

            IndexStats indexStats = store.maintenance().send(new GetIndexStatisticsOperation("MultiMapLoadWithRepetition"));
            assertThat(indexStats)
                    .isNotNull();
            assertThat(indexStats.getReferencedCollections())
                    .containsExactly("Item3s");
        }
    }

    private void arrange(IDocumentStore store, AbstractIndexCreationTaskBase<?> index, boolean disableIndex) throws Exception {
        index.execute(store);

        try (IDocumentSession session = store.openSession()) {
            session.store(new Item1("item2/1", "item3/1", "Item1/1"), "item1/1");
            session.store(new Item2("item2/1", "item3/1"), "item2/1");
            session.store(new Item3("item2/1", "item3/1"), "item3/1");
            session.saveChanges();
        }

        waitForIndexing(store);

        if (disableIndex) {
            store.maintenance().send(new DisableIndexOperation(index.getIndexName()));
        }
    }

    public static class Item1 {
        private String refId2;
        private String refId3;
        private String prop;

        public Item1() {
        }

        public Item1(String refId2, String refId3, String prop) {
            this.refId2 = refId2;
            this.refId3 = refId3;
            this.prop = prop;
        }

        public String getRefId2() {
            return refId2;
        }

        public void setRefId2(String refId2) {
            this.refId2 = refId2;
        }

        public String getRefId3() {
            return refId3;
        }

        public void setRefId3(String refId3) {
            this.refId3 = refId3;
        }

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    public static class Item2 {
        private String refId;
        private String prop;

        public Item2() {
        }

        public Item2(String refId, String prop) {
            this.refId = refId;
            this.prop = prop;
        }

        public String getRefId() {
            return refId;
        }

        public void setRefId(String refId) {
            this.refId = refId;
        }

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    public static class Item3 extends Item2 {
        public Item3() {
        }

        public Item3(String refId, String prop) {
            super(refId, prop);
        }
    }

    public static class SingleCollectionLoad extends AbstractIndexCreationTask {
        public SingleCollectionLoad() {
            map = "from doc in docs.Item1s " +
                    "let loadDocument = LoadDocument(doc.RefId2, \"Item2s\") " +
                    "select new { Current = doc.Prop, Loaded2 = loadDocument.Prop }";
            storeAllFields(FieldStorage.YES);
        }
    }

    public static class MultipleCollectionLoad extends AbstractIndexCreationTask {
        public MultipleCollectionLoad() {
            map = "from doc in docs.Item1s " +
                    "let loadDocument = LoadDocument(doc.RefId2, \"Item2s\") " +
                    "let loadDocument2 = LoadDocument(doc.RefId3, \"Item3s\") " +
                    "select new { Current = doc.Prop, Loaded2 = loadDocument.Prop, Loaded3 = loadDocument2.Prop }";
            storeAllFields(FieldStorage.YES);
        }
    }

    public static class MultiMapLoad extends AbstractMultiMapIndexCreationTask {
        public MultiMapLoad() {
            addMap("from doc in docs.Item1s " +
                    "let loadDocument3 = LoadDocument(doc.RefId2, \"Item2s\") " +
                    "select new { Current = doc.Prop, Loaded = loadDocument3.Prop }");

            addMap("from doc in docs.Item2s " +
                    "let loadDocument = LoadDocument(doc.RefId, \"Item3s\") " +
                    "select new { Current = doc.Prop, Loaded = loadDocument.Prop }");

            storeAllFields(FieldStorage.YES);
        }
    }

    public static class MultiMapLoadWithRepetition extends AbstractMultiMapIndexCreationTask {
        public MultiMapLoadWithRepetition() {
            addMap("from doc in docs.Item1s " +
                    "let loadDocument3 = LoadDocument(doc.RefId2, \"Item3s\") " +
                    "select new { Current = doc.Prop, Loaded = loadDocument3.Prop }");

            addMap("from doc in docs.Item2s " +
                    "let loadDocument = LoadDocument(doc.RefId, \"Item3s\") " +
                    "select new { Current = doc.Prop, Loaded = loadDocument.Prop }");

            storeAllFields(FieldStorage.YES);
        }
    }
}
