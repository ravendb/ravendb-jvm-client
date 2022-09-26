package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_17872Test extends RemoteTestBase {

    @Test
    public void compareExchangeMetadata_Create_WithoutPropChange() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            final String id = "testObjs/0";
            final String metadataPropName = "RandomProp";
            final String metadataValue = "RandomValue";

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                TestObj entity = new TestObj();
                session.advanced().clusterTransaction().createCompareExchangeValue(id, entity);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                // entity.Value.Prop = "Changed"; //without this line the session doesn't send an update to the compare exchange
                entity.getMetadata().put(metadataPropName, metadataValue);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                assertThat(entity.getMetadata())
                        .containsKey(metadataPropName);
                assertThat(entity.getMetadata().getString(metadataPropName))
                        .isEqualTo(metadataValue);
            }
        }
    }

    @Test
    public void compareExchangeMetadata_CreateAndTryToChangeToSameVal() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            final String id = "testObjs/0";
            final String metadataPropName = "RandomProp";
            final String metadataValue = "RandomValue";

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                TestObj entity = new TestObj();
                session.advanced().clusterTransaction().createCompareExchangeValue(id, entity);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                entity.getMetadata().put(metadataPropName, metadataValue);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                assertThat(entity.getMetadata())
                        .containsKey(metadataPropName);
                assertThat(entity.getMetadata().getString(metadataPropName))
                        .isEqualTo(metadataValue);
            }
        }
    }

    @Test
    public void compareExchangeMetadata_CreateAndTryToChangeToDifferentVal() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            final String id = "testObjs/0";
            final String metadataPropName = "RandomProp";
            final String metadataValue = "RandomValue";

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                TestObj entity = new TestObj();
                session.advanced().clusterTransaction().createCompareExchangeValue(id, entity);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                // entity.Value.Prop = "Changed"; //without this line the session doesn't send an update to the compare exchange
                entity.getMetadata().put(metadataPropName, metadataValue);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                entity.getMetadata().put(metadataPropName, metadataValue + "1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                assertThat(entity.getMetadata())
                        .containsKey(metadataPropName);

                assertThat(entity.getMetadata().getString(metadataPropName))
                        .isEqualTo(metadataValue + "1");
            }
        }
    }

    @Test
    public void compareExchangeMetadata_DontDoAnything() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String id = "testObjs/0";

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                TestObj entity = new TestObj();
                session.advanced().clusterTransaction().createCompareExchangeValue(id, entity);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                entity.getValue().prop = "Changed";
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);

                assertThat(entity.getMetadata())
                        .isEmpty();
                assertThat(entity.getValue().getProp())
                        .isEqualTo("Changed");
            }
        }
    }

    @Test
    public void compareExchangeMetadata_CreateAndTryToRemoveAndAddVal() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            String id = "testObjs/0";
            String metadataPropName = "RandomProp";
            String metadataValue = "RandomValue";

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                TestObj entity = new TestObj();
                session.advanced().clusterTransaction().createCompareExchangeValue(id, entity);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                // entity.Value.Prop = "Changed"; //without this line the session doesn't send an update to the compare exchange
                entity.getMetadata().put(metadataPropName, metadataValue);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                assertThat(entity.getMetadata().remove(metadataPropName))
                        .isNotNull();
                entity.getMetadata().put(metadataPropName + "1", metadataValue + "1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                assertThat(entity.getMetadata().size())
                        .isOne();
                assertThat(entity.getMetadata())
                        .containsKey(metadataPropName + "1");
                assertThat(entity.getMetadata().get(metadataPropName + "1"))
                        .isEqualTo(metadataValue + "1");
            }
        }
    }

    @Test
    public void compareExchangeMetadata_CreateAndTryToRemove() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String id = "testObjs/0";
            String metadataPropName = "RandomProp";
            String metadataValue = "RandomValue";

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                TestObj entity = new TestObj();
                session.advanced().clusterTransaction().createCompareExchangeValue(id, entity);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                // entity.Value.Prop = "Changed"; //without this line the session doesn't send an update to the compare exchange
                entity.getMetadata().put(metadataPropName, metadataValue);
                entity.getMetadata().put(metadataPropName + "1", metadataValue + "1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                assertThat(entity.getMetadata().remove(metadataPropName))
                        .isNotNull();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                assertThat(entity.getMetadata())
                        .hasSize(1);
                assertThat(entity.getMetadata())
                        .containsKey(metadataPropName + "1");
                assertThat(entity.getMetadata().get(metadataPropName + "1"))
                        .isEqualTo(metadataValue + "1");
            }
        }
    }

    @Test
    public void compareExchangeNestedMetadata_Create_WithoutPropChange() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String id = "testObjs/0";

            Map<String, Long> dictionary = new HashMap<>();
            dictionary.put("bbbb", 2L);

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                TestObj entity = new TestObj();
                session.advanced().clusterTransaction().createCompareExchangeValue(id, entity);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                // entity.Value.Prop = "Changed"; //without this line the session doesn't send an update to the compare exchange
                entity.getMetadata().put("Custom-Metadata", dictionary);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                IMetadataDictionary metadata = entity.getMetadata();
                IMetadataDictionary dictionary1 = metadata.getObject("Custom-Metadata");
                assertThat(dictionary1.getLong("bbbb"))
                        .isEqualTo(2);
            }
        }
    }

    @Test
    public void compareExchangeDoubleNestedMetadata_Create_WithoutPropChange() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            String id = "testObjs/0";

            Map<String, Map<String, Integer>> dictionary = new HashMap<>();

            Map<String, Integer> sub1 = new HashMap<>();
            sub1.put("aaaa", 1);

            Map<String, Integer> sub2 = new HashMap<>();
            sub2.put("bbbb", 2);

            dictionary.put("123", sub1);
            dictionary.put("321", sub2);

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                TestObj entity = new TestObj();
                session.advanced().clusterTransaction().createCompareExchangeValue(id, entity);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                // entity.Value.Prop = "Changed"; //without this line the session doesn't send an update to the compare exchange
                entity.getMetadata().put("Custom-Metadata", dictionary);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                CompareExchangeValue<TestObj> entity = session.advanced().clusterTransaction().getCompareExchangeValue(TestObj.class, id);
                IMetadataDictionary metadata = entity.getMetadata();
                IMetadataDictionary dictionaryLevel1 = metadata.getObject("Custom-Metadata");


                IMetadataDictionary dictionaryLevel2 = dictionaryLevel1.getObject("123");
                assertThat(dictionaryLevel2.getLong("aaaa"))
                        .isEqualTo(1);

                dictionaryLevel2 = dictionaryLevel1.getObject("321");
                assertThat(dictionaryLevel2.getLong("bbbb"))
                        .isEqualTo(2);
            }
        }
    }

    public static class TestObj {
        private String id;
        private String prop;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }
    }
}
