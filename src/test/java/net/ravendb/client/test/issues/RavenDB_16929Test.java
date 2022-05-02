package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_16929Test extends RemoteTestBase {

    @Test
    public void documentWithStringWithNullCharacterAtEndShouldNotHaveChangeOnLoad() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                TestDoc doc = new TestDoc();
                doc.setId("doc/1");
                doc.setDescriptionChar('a');
                doc.setDescription("TestString\0");
                session.store(doc);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TestDoc doc = session.load(TestDoc.class, "doc/1");
                String t = doc.getDescription();
                assertThat(session.advanced().hasChanges())
                        .isFalse();
                assertThat(session.advanced().hasChanged(doc))
                        .isFalse();
            }
        }
    }

    @Test
    public void documentWithEmptyCharShouldNotHaveChangeOnLoad() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                TestDoc doc = new TestDoc();
                doc.setId("doc/1");
                session.store(doc);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TestDoc doc = session.load(TestDoc.class, "doc/1");
                assertThat(session.advanced().hasChanges())
                        .isFalse();
                assertThat(session.advanced().hasChanged(doc))
                        .isFalse();
            }
        }
    }

    public static class TestDoc {
        private char descriptionChar;
        private String id;
        private String description;

        public char getDescriptionChar() {
            return descriptionChar;
        }

        public void setDescriptionChar(char descriptionChar) {
            this.descriptionChar = descriptionChar;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
