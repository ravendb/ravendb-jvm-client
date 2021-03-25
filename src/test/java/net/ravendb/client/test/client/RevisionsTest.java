package net.ravendb.client.test.client;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.json.JsonArrayResult;
import net.ravendb.client.json.MetadataAsDictionary;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RevisionsTest extends RemoteTestBase {
    @Test
    public void revisions() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            setupRevisions(store, false, 4);


            for (int i = 0; i < 4; i++) {
                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    user.setName("user" + (i + 1));
                    session.store(user, "users/1");
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<User> allRevisions = session.advanced().revisions().getFor(User.class, "users/1");
                assertThat(allRevisions)
                        .hasSize(4);
                assertThat(allRevisions.stream().map(x -> x.getName()).collect(Collectors.toList()))
                        .containsSequence("user4", "user3", "user2" , "user1");

                List<User> revisionsSkipFirst = session.advanced().revisions().getFor(User.class, "users/1", 1);
                assertThat(revisionsSkipFirst)
                        .hasSize(3);
                assertThat(revisionsSkipFirst.stream().map(x -> x.getName()).collect(Collectors.toList()))
                        .containsSequence("user3", "user2" , "user1");

                List<User> revisionsSkipFirstTakeTwo = session.advanced().revisions().getFor(User.class, "users/1", 1, 2);
                assertThat(revisionsSkipFirstTakeTwo)
                        .hasSize(2);
                assertThat(revisionsSkipFirstTakeTwo.stream().map(x -> x.getName()).collect(Collectors.toList()))
                        .containsSequence("user3", "user2" );

                List<MetadataAsDictionary> allMetadata = session.advanced().revisions().getMetadataFor("users/1");
                assertThat(allMetadata)
                        .hasSize(4);

                List<MetadataAsDictionary> metadataSkipFirst = session.advanced().revisions().getMetadataFor("users/1", 1);
                assertThat(metadataSkipFirst)
                        .hasSize(3);

                List<MetadataAsDictionary> metadataSkipFirstTakeTwo = session.advanced().revisions().getMetadataFor("users/1", 1, 2);
                assertThat(metadataSkipFirstTakeTwo)
                        .hasSize(2);


                User user = session.advanced().revisions().get(User.class, (String) metadataSkipFirst.get(0).get(Constants.Documents.Metadata.CHANGE_VECTOR));
                assertThat(user.getName())
                        .isEqualTo("user3");
            }
        }
    }

    @Test
    public void canListRevisionsBin() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setupRevisions(store, false, 4);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("user1");
                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.delete("users/1");
                session.saveChanges();
            }

            GetRevisionsBinEntryCommand revisionsBinEntryCommand = new GetRevisionsBinEntryCommand(Long.MAX_VALUE, 20);
            store.getRequestExecutor().execute(revisionsBinEntryCommand);

            JsonArrayResult result = revisionsBinEntryCommand.getResult();
            assertThat(result.getResults())
                    .hasSize(1);

            assertThat(result.getResults().get(0).get("@metadata").get("@id").asText())
                    .isEqualTo("users/1");
        }
    }

    @Test
    public void canGetRevisionsByChangeVectors() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "users/1";

            setupRevisions(store, false, 100);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Fitzchak");
                session.store(user, id);
                session.saveChanges();
            }

            for (int i = 0; i < 10; i++) {
                try (IDocumentSession session = store.openSession()) {
                    Company user = session.load(Company.class, id);
                    user.setName("Fitzchak" + i);
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<MetadataAsDictionary> revisionsMetadata = session.advanced().revisions().getMetadataFor(id);
                assertThat(revisionsMetadata)
                        .hasSize(11);

                List<String> changeVectors = revisionsMetadata
                        .stream()
                        .map(x -> x.getString(Constants.Documents.Metadata.CHANGE_VECTOR))
                        .collect(Collectors.toList());
                changeVectors.add("NotExistsChangeVector");

                Map<String, User> revisions = session.advanced().revisions().get(User.class, changeVectors.toArray(new String[0]));
                assertThat(revisions.get("NotExistsChangeVector"))
                        .isNull();

                assertThat(session.advanced().revisions().get(User.class, "NotExistsChangeVector"))
                        .isNull();
            }
        }
    }

    @Test
    public void collectionCaseSensitiveTest1() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "user/1";
            RevisionsConfiguration configuration = new RevisionsConfiguration();

            RevisionsCollectionConfiguration collectionConfiguration = new RevisionsCollectionConfiguration();
            collectionConfiguration.setDisabled(false);

            configuration.setCollections(new HashMap<>());
            configuration.getCollections().put("uSErs", collectionConfiguration);

            store.maintenance().send(new ConfigureRevisionsOperation(configuration));

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("raven");
                session.store(user, id);
                session.saveChanges();
            }

            for (int i = 0; i < 10; i++) {
                try (IDocumentSession session = store.openSession()) {
                    Company user = session.load(Company.class, id);
                    user.setName("raven " + i);
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<MetadataAsDictionary> revisionsMetadata = session.advanced().revisions().getMetadataFor(id);
                assertThat(revisionsMetadata)
                        .hasSize(11);
            }
        }
    }

    @Test
    public void collectionCaseSensitiveTest2() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "uSEr/1";
            RevisionsConfiguration configuration = new RevisionsConfiguration();

            RevisionsCollectionConfiguration collectionConfiguration = new RevisionsCollectionConfiguration();
            collectionConfiguration.setDisabled(false);

            configuration.setCollections(new HashMap<>());
            configuration.getCollections().put("users", collectionConfiguration);

            store.maintenance().send(new ConfigureRevisionsOperation(configuration));

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("raven");
                session.store(user, id);
                session.saveChanges();
            }

            for (int i = 0; i < 10; i++) {
                try (IDocumentSession session = store.openSession()) {
                    Company user = session.load(Company.class, id);
                    user.setName("raven " + i);
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<MetadataAsDictionary> revisionsMetadata = session.advanced().revisions().getMetadataFor(id);
                assertThat(revisionsMetadata)
                        .hasSize(11);
            }
        }
    }

    @Test
    public void collectionCaseSensitiveTest3() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RevisionsConfiguration configuration = new RevisionsConfiguration();

            RevisionsCollectionConfiguration c1 = new RevisionsCollectionConfiguration();
            c1.setDisabled(false);

            RevisionsCollectionConfiguration c2 = new RevisionsCollectionConfiguration();
            c2.setDisabled(false);

            configuration.setCollections(new HashMap<>());
            configuration.getCollections().put("users", c1);
            configuration.getCollections().put("USERS", c2);

            assertThatThrownBy(() -> {
                store.maintenance().send(new ConfigureRevisionsOperation(configuration));
            })
                    .isInstanceOf(RavenException.class);
        }
    }
}
