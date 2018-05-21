package net.ravendb.client.test.server.patching;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.operations.PatchByQueryOperation;
import net.ravendb.client.documents.operations.PatchOperation;
import net.ravendb.client.documents.operations.PatchRequest;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AdvancedPatchingTest extends RemoteTestBase {

    public static class CustomType {
        private String id;
        private String owner;
        private int value;
        private List<String> comments;
        private Date date;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public List<String> getComments() {
            return comments;
        }

        public void setComments(List<String> comments) {
            this.comments = comments;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    @Test
    public void testWithVariables() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                CustomType customType = new CustomType();
                customType.setOwner("me");
                session.store(customType, "customTypes/1");
                session.saveChanges();
            }

            PatchRequest patchRequest = new PatchRequest();
            patchRequest.setScript("this.owner = args.v1");
            patchRequest.setValues(Collections.singletonMap("v1", "not-me"));

            PatchOperation patchOperation = new PatchOperation("customTypes/1", null, patchRequest);
            store.operations().send(patchOperation);

            try (IDocumentSession session = store.openSession()) {
                CustomType loaded = session.load(CustomType.class, "customTypes/1");
                assertThat(loaded.getOwner())
                        .isEqualTo("not-me");
            }
        }
    }

    @Test
    public void canCreateDocumentsIfPatchingAppliedByIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {
                CustomType type1 = new CustomType();
                type1.setId("Item/1");
                type1.setValue(1);

                CustomType type2 = new CustomType();
                type2.setId("Item/2");
                type2.setValue(2);

                newSession.store(type1);
                newSession.store(type2);
                newSession.saveChanges();
            }

            IndexDefinition def1 = new IndexDefinition();
            def1.setName("TestIndex");
            def1.setMaps(Sets.newHashSet("from doc in docs.CustomTypes select new { doc.value }"));


            store.maintenance().send(new PutIndexesOperation(def1));

            try (IDocumentSession session = store.openSession()) {
                session
                        .advanced().documentQuery(CustomType.class, "TestIndex", null, false)
                        .waitForNonStaleResults()
                        .toList();
            }

            Operation operation = store.operations().sendAsync(new PatchByQueryOperation("FROM INDEX 'TestIndex' WHERE value = 1 update { put('NewItem/3', {'copiedValue': this.value });}"));

            operation.waitForCompletion();

            try (IDocumentSession session = store.openSession()) {
                ObjectNode jsonDocument = session.load(ObjectNode.class, "NewItem/3");
                assertThat(jsonDocument.get("copiedValue").asText())
                        .isEqualTo("1");
            }
        }
    }
}
