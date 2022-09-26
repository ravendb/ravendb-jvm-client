package net.ravendb.client.test.linq;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.json.MetadataAsDictionary;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class CanQueryAndIncludeRevisionsTest extends RemoteTestBase {

    @Test
    public void query_IncludeAllQueryFunctionality() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            List<String> cvList = new ArrayList<String>();

            String id = "users/rhino";

            setupRevisions(store, false, 5);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Omer");
                session.store(user, id);
                session.saveChanges();
            }

            String changeVector;
            Date beforeDateTime = new Date();

            try (IDocumentSession session = store.openSession()) {
                List<MetadataAsDictionary> metadatas = session.advanced().revisions().getMetadataFor(id);
                assertThat(metadatas)
                        .hasSize(1);

                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);

                session.advanced().patch(id, "firstRevision", changeVector);

                session.saveChanges();

                cvList.add(changeVector);

                metadatas = session.advanced().revisions().getMetadataFor(id);

                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);

                cvList.add(changeVector);

                session.advanced().patch(id, "secondRevision", changeVector);

                session.saveChanges();

                metadatas = session.advanced().revisions().getMetadataFor(id);

                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);

                cvList.add(changeVector);

                session.advanced().patch(id, "changeVectors", cvList);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<User> query = session.query(User.class)
                        .include(builder -> builder
                                .includeRevisions("changeVectors")
                                .includeRevisions("firstRevision")
                                .includeRevisions("secondRevision"))
                        .waitForNonStaleResults();

                query.toList();

                User revision1 = session.advanced().revisions().get(User.class, cvList.get(0));
                User revision2 = session.advanced().revisions().get(User.class, cvList.get(1));
                User revision3 = session.advanced().revisions().get(User.class, cvList.get(2));

                assertThat(revision1)
                        .isNotNull();
                assertThat(revision1.getFirstRevision())
                        .isNull();
                assertThat(revision1.getSecondRevision())
                        .isNull();
                assertThat(revision1.getChangeVectors())
                        .isNull();

                assertThat(revision2)
                        .isNotNull();
                assertThat(revision2.getFirstRevision())
                        .isNotNull();
                assertThat(revision2.getSecondRevision())
                        .isNull();
                assertThat(revision2.getChangeVectors())
                        .isNull();

                assertThat(revision3)
                        .isNotNull();
                assertThat(revision3.getFirstRevision())
                        .isNotNull();
                assertThat(revision3.getSecondRevision())
                        .isNotNull();
                assertThat(revision3.getChangeVectors())
                        .isNull();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void load_IncludeBuilder_IncludeRevisionByChangeVector() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String id = "users/rhino";

            setupRevisions(store, false, 5);

            String changeVector;

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Omer");
                session.store(user, id);
                session.saveChanges();

                List<MetadataAsDictionary> metadatas = session.advanced().revisions().getMetadataFor(id);
                assertThat(metadatas)
                        .hasSize(1);

                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);

                session.advanced().patch(id, "changeVector", changeVector);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User query = session.load(User.class, id, builder -> builder.includeRevisions("changeVector"));

                User revision = session.advanced().revisions().get(User.class, changeVector);

                assertThat(revision)
                        .isNotNull();
                assertThat(revision.getFirstRevision())
                        .isNull();
                assertThat(revision.getSecondRevision())
                        .isNull();
                assertThat(revision.getChangeVectors())
                        .isNull();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void load_IncludeBuilder_IncludeRevisionByChangeVectors() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            List<String> cvList = new ArrayList<String>();

            String id = "users/rhino";

            setupRevisions(store, false, 5);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Omer");
                session.store(user, id);
                session.saveChanges();
            }

            String changeVector;

            try (IDocumentSession session = store.openSession()) {
                List<MetadataAsDictionary> metadatas = session.advanced().revisions().getMetadataFor(id);
                assertThat(metadatas)
                        .hasSize(1);

                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);
                session.advanced().patch(id, "firstRevision", changeVector);
                session.saveChanges();
                cvList.add(changeVector);

                metadatas = session.advanced().revisions().getMetadataFor(id);
                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);
                cvList.add(changeVector);

                session.advanced().patch(id, "secondRevision", changeVector);

                session.saveChanges();
                metadatas = session.advanced().revisions().getMetadataFor(id);
                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);

                cvList.add(changeVector);
                session.advanced().patch(id, "thirdRevision", changeVector);
                session.advanced().patch(id, "changeVectors", cvList);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User query = session.load(User.class, id, builder -> builder.includeRevisions("changeVectors"));

                User revision1 = session.advanced().revisions().get(User.class, cvList.get(0));
                User revision2 = session.advanced().revisions().get(User.class, cvList.get(1));
                User revision3 = session.advanced().revisions().get(User.class, cvList.get(2));

                assertThat(revision1)
                        .isNotNull();
                assertThat(revision1.getFirstRevision())
                        .isNull();
                assertThat(revision1.getSecondRevision())
                        .isNull();
                assertThat(revision1.getChangeVectors())
                        .isNull();

                assertThat(revision2)
                        .isNotNull();
                assertThat(revision2.getFirstRevision())
                        .isNotNull();
                assertThat(revision2.getSecondRevision())
                        .isNull();
                assertThat(revision2.getChangeVectors())
                        .isNull();

                assertThat(revision3)
                        .isNotNull();
                assertThat(revision3.getFirstRevision())
                        .isNotNull();
                assertThat(revision3.getSecondRevision())
                        .isNotNull();
                assertThat(revision3.getChangeVectors())
                        .isNull();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void load_IncludeBuilder_IncludeRevisionsByProperty_ChangeVectorAndChangeVectors() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            List<String> cvList = new ArrayList<String>();

            String id = "users/rhino";

            setupRevisions(store, false, 5);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Omer");
                session.store(user, id);
                session.saveChanges();
            }

            String changeVector;

            try (IDocumentSession session = store.openSession()) {
                List<MetadataAsDictionary> metadatas = session.advanced().revisions().getMetadataFor(id);
                assertThat(metadatas)
                        .hasSize(1);

                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);
                session.advanced().patch(id, "firstRevision", changeVector);
                session.saveChanges();
                cvList.add(changeVector);

                metadatas = session.advanced().revisions().getMetadataFor(id);
                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);
                cvList.add(changeVector);
                session.advanced().patch(id, "secondRevision", changeVector);
                session.saveChanges();

                metadatas = session.advanced().revisions().getMetadataFor(id);
                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);
                cvList.add(changeVector);
                session.advanced().patch(id, "changeVectors", cvList);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.load(User.class, id, builder ->
                        builder.includeRevisions("changeVectors")
                                .includeRevisions("firstRevision")
                                .includeRevisions("secondRevision"));

                User revision1 = session.advanced().revisions().get(User.class, cvList.get(0));
                User revision2 = session.advanced().revisions().get(User.class, cvList.get(1));
                User revision3 = session.advanced().revisions().get(User.class, cvList.get(2));
                Map<String, User> revisions =
                        session.advanced().revisions().get(User.class, cvList.toArray(new String[0]));

                assertThat(revision1)
                        .isNotNull();
                assertThat(revision1.getFirstRevision())
                        .isNull();
                assertThat(revision1.getSecondRevision())
                        .isNull();
                assertThat(revision1.getChangeVectors())
                        .isNull();
                assertThat(revision2)
                        .isNotNull();
                assertThat(revision2.getFirstRevision())
                        .isNotNull();
                assertThat(revision2.getSecondRevision())
                        .isNull();
                assertThat(revision2.getChangeVectors())
                        .isNull();
                assertThat(revision3)
                        .isNotNull();
                assertThat(revision3.getFirstRevision())
                        .isNotNull();
                assertThat(revision3.getSecondRevision())
                        .isNotNull();
                assertThat(revision3.getChangeVectors())
                        .isNull();
                assertThat(revisions)
                        .isNotNull();

                assertThat(revisions)
                        .hasSize(3);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void load_IncludeBuilder_IncludeRevisionByDateTime_VerifyUtc() throws Exception {
        String changeVector;

        try (DocumentStore store = getDocumentStore()) {
            String id = "users/rhino";

            setupRevisions(store, false, 5);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Omer");
                session.store(user, id);
                session.saveChanges();

                List<MetadataAsDictionary> metadatas = session.advanced().revisions().getMetadataFor(id);
                assertThat(metadatas)
                        .hasSize(1);

                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);
                session.advanced().patch(id, "changeVector", changeVector);
                session.advanced().patch(id, "changeVectors", Arrays.asList(changeVector));
                session.saveChanges();
            }

            Date dateTime = new Date();

            Thread.sleep(2);

            try (IDocumentSession session = store.openSession()) {
                User query = session.load(User.class, id, builder -> builder
                        .includeRevisions(dateTime)
                        .includeRevisions("changeVector")
                        .includeRevisions("changeVectors"));

                User revision = session.advanced().revisions().get(User.class, id, dateTime);
                User revision2 = session.advanced().revisions().get(User.class, changeVector);

                assertThat(query)
                        .isNotNull();
                assertThat(revision)
                        .isNotNull();
                assertThat(revision2)
                        .isNotNull();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void query_IncludeBuilder_IncludeRevisionBefore() throws Exception {
        String changeVector;

        try (DocumentStore store = getDocumentStore()) {
            String id = "users/rhino";

            setupRevisions(store, true, 5);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Omer");
                session.store(user, id);
                session.saveChanges();

                List<MetadataAsDictionary> metadatas = session.advanced().revisions().getMetadataFor(id);
                assertThat(metadatas)
                        .hasSize(1);
                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);
            }

            Date beforeDateTime = new Date();

            Thread.sleep(2);

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<User> query = session.query(User.class)
                        .waitForNonStaleResults()
                        .include(builder -> builder.includeRevisions(beforeDateTime));

                List<User> users = query.toList();

                User revision = session.advanced().revisions().get(User.class, changeVector);
                assertThat(users)
                        .isNotNull();
                assertThat(users)
                        .hasSize(1);
                assertThat(revision)
                        .isNotNull();
                assertThat(revision.getName())
                        .isNotNull();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void query_RawQueryChangeVectorInsidePropertyWithIndex() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            String id = "users/rhino";

            setupRevisions(store, true, 5);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Omer");
                session.store(user, id);
                session.saveChanges();
            }

            String changeVector;

            try (IDocumentSession session = store.openSession()) {
                List<MetadataAsDictionary> metadatas = session.advanced().revisions().getMetadataFor(id);
                assertThat(metadatas)
                        .hasSize(1);

                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);

                session.advanced().patch(id, "changeVector", changeVector);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<User> query = session.advanced()
                        .rawQuery(User.class, "from Users where name = 'Omer' include revisions($p0)")
                        .addParameter("p0", "changeVector")
                        .waitForNonStaleResults()
                        .toList();

                User revision = session.advanced().revisions().get(User.class, changeVector);
                assertThat(revision)
                        .isNotNull();
                assertThat(revision.getName())
                        .isNotNull();
                assertThat(query)
                        .isNotNull();
                assertThat(query)
                        .hasSize(1);
                assertThat(query.get(0).getName())
                        .isNotNull();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void query_RawQueryGetRevisionBeforeDateTime() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String id = "users/rhino";

            setupRevisions(store, true, 5);

            String changeVector;

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Omer");
                session.store(user, id);
                session.saveChanges();

                List<MetadataAsDictionary> metadatas = session.advanced().revisions().getMetadataFor(id);
                assertThat(metadatas)
                        .hasSize(1);

                changeVector = metadatas.get(0).getString(Constants.Documents.Metadata.CHANGE_VECTOR);
            }

            try (IDocumentSession session = store.openSession()) {
                Date getRevisionsBefore = new Date();
                List<User> query = session.advanced().rawQuery(User.class, "from Users include revisions($p0)")
                        .addParameter("p0", getRevisionsBefore)
                        .waitForNonStaleResults()
                        .toList();

                User revision = session.advanced().revisions().get(User.class, changeVector);

                assertThat(query)
                        .isNotNull();
                assertThat(query)
                        .hasSize(1);
                assertThat(query.get(0).getName())
                        .isNotNull();
                assertThat(revision)
                        .isNotNull();
                assertThat(revision.getName())
                        .isNotNull();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    public static class NameIndex extends AbstractIndexCreationTask {
        public NameIndex() {
            map = "from u in docs.Users select new { u.name }";
        }
    }


    public static class User {
        private String name;
        private String changeVector;
        private String firstRevision;
        private String secondRevision;
        private String thirdRevision;
        private List<String> changeVectors;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getChangeVector() {
            return changeVector;
        }

        public void setChangeVector(String changeVector) {
            this.changeVector = changeVector;
        }

        public String getFirstRevision() {
            return firstRevision;
        }

        public void setFirstRevision(String firstRevision) {
            this.firstRevision = firstRevision;
        }

        public String getSecondRevision() {
            return secondRevision;
        }

        public void setSecondRevision(String secondRevision) {
            this.secondRevision = secondRevision;
        }

        public String getThirdRevision() {
            return thirdRevision;
        }

        public void setThirdRevision(String thirdRevision) {
            this.thirdRevision = thirdRevision;
        }

        public List<String> getChangeVectors() {
            return changeVectors;
        }

        public void setChangeVectors(List<String> changeVectors) {
            this.changeVectors = changeVectors;
        }
    }

}
