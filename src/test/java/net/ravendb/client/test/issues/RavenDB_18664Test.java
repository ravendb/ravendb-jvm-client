package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_18664Test extends RemoteTestBase {

    @Test
    public void givenADocument_WhenAnEmptyListIsPassedToCheckIfIdsExist_QueryShouldReturnZeroResults() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                createTestDocument(session);
                session.saveChanges();
            }

            List<String> emptyList = new ArrayList<>();

            try (IDocumentSession session = store.openSession()) {
                int queryCount = session.query(TestDocument.class)
                        .whereIn("id", emptyList)
                        .count();

                assertThat(queryCount)
                        .isZero();
            }

        }


    }

    private static void createTestDocument(IDocumentSession session) {
        TestDocument testDoc = new TestDocument();
        testDoc.setComment("TestDoc1");

        session.store(testDoc);
    }

    public static class TestDocument {
        private String id;
        private String comment;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

}
