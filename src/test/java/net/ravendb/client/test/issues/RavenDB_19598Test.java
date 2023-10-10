package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_19598Test extends RemoteTestBase {


    @Test
    public void testRefreshOverload() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                SimpleDoc[] docs = new SimpleDoc[3];
                docs[0] = new SimpleDoc();
                docs[0].setId("TestDoc0");
                docs[0].setName("State0");

                docs[1] = new SimpleDoc();
                docs[1].setId("TestDoc1");
                docs[1].setName("State1");

                docs[2] = new SimpleDoc();
                docs[2].setId("TestDoc2");
                docs[2].setName("State2");

                for (SimpleDoc doc : docs) {
                    session.store(doc);
                }

                session.saveChanges();

                // loading the stored docs and name field equality assertions
                SimpleDoc sd = session.load(SimpleDoc.class, docs[0].getId());
                assertThat(sd.getName())
                        .isEqualTo(docs[0].getName());

                SimpleDoc sd1 = session.load(SimpleDoc.class, docs[1].getId());
                assertThat(sd1.getName())
                        .isEqualTo(docs[1].getName());

                SimpleDoc sd2 = session.load(SimpleDoc.class, docs[2].getId());
                assertThat(sd2.getName())
                        .isEqualTo(docs[2].getName());

                // changing the name fields and saving the changes
                sd.setName("grossesNashorn");
                sd1.setName("kleinesNashorn");
                sd2.setName("krassesNashorn");
                session.saveChanges();

                // overriding locally the name fields (without saving)
                sd.setName("schwarzeKraehe");
                sd1.setName("weisseKraehe");
                sd2.setName("gelbeKraehe");

                session.advanced().refresh(Arrays.asList(sd, sd1, sd2));

                // equality assertion of current names and pre-override names
                assertThat(sd.getName())
                        .isEqualTo("grossesNashorn");
                assertThat(sd1.getName())
                        .isEqualTo("kleinesNashorn");
                assertThat(sd2.getName())
                        .isEqualTo("krassesNashorn");
            }
        }
    }

    @Test
    public void testRefreshOverloadSameDocs() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                // creating document and store it

                SimpleDoc doc = new SimpleDoc();
                doc.setId("TestDoc0");
                doc.setName("State0");

                session.store(doc);
                session.saveChanges();

                // loading the stored doc and name field equality assertions
                SimpleDoc sd = session.load(SimpleDoc.class, doc.id);
                assertThat(sd.getName())
                        .isEqualTo(doc.getName());

                // changing the name field and saving the changes
                sd.setName("grossesNashorn");
                session.saveChanges();

                // overriding locally the name field (without saving)
                sd.setName("schwarzeKraehe");

                session.advanced().refresh(Arrays.asList(sd, sd, sd));

                // equality assertion of current names and pre-override names
                assertThat(sd.getName())
                        .isEqualTo("grossesNashorn");

            }
        }
    }

    @Test
    public void testRefreshOverloadWithDocDeletion() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                // creating document and store it

                SimpleDoc doc = new SimpleDoc();
                doc.setId("TestDoc0");
                doc.setName("State0");

                SimpleDoc doc1 = new SimpleDoc();
                doc1.setId("TestDoc1");
                doc1.setName("State1");

                session.store(doc);
                session.store(doc1);
                session.saveChanges();

                // loading the stored doc and name field equality assertions
                SimpleDoc sd = session.load(SimpleDoc.class, doc.getId());
                assertThat(sd.getName())
                        .isEqualTo(doc.getName());

                SimpleDoc sd1 = session.load(SimpleDoc.class, doc1.getId());
                assertThat(sd1.getName())
                        .isEqualTo(doc1.getName());

                // changing the name field and saving the changes
                sd.setName("grossesNashorn");
                sd1.setName("kleinesNashorn");
                session.saveChanges();

                // overriding locally the name field (without saving)
                sd.setName("schwarzeKraehe");
                sd1.setName("weisseKraehe");

                try (IDocumentSession session2 = store.openSession()) {
                    session2.delete(sd.getId());
                    session2.saveChanges();
                }

                assertThatThrownBy(() -> session.advanced().refresh(Arrays.asList(sd, sd1)))
                        .isExactlyInstanceOf(IllegalStateException.class);
            }
        }
    }

    public static class SimpleDoc {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
