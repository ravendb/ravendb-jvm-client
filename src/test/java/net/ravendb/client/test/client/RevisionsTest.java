package net.ravendb.client.test.client;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.commands.GetRevisionsBinEntryCommand;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.documents.operations.revisions.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.json.JsonArrayResult;
import net.ravendb.client.json.MetadataAsDictionary;
import org.junit.jupiter.api.Test;

import java.util.*;
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

                List<IMetadataDictionary> allMetadata = session.advanced().revisions().getMetadataFor("users/1");
                assertThat(allMetadata)
                        .hasSize(4);

                List<IMetadataDictionary> metadataSkipFirst = session.advanced().revisions().getMetadataFor("users/1", 1);
                assertThat(metadataSkipFirst)
                        .hasSize(3);

                List<IMetadataDictionary> metadataSkipFirstTakeTwo = session.advanced().revisions().getMetadataFor("users/1", 1, 2);
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

            GetRevisionsBinEntryCommand revisionsBinEntryCommand = new GetRevisionsBinEntryCommand(0, 20);
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
                List<IMetadataDictionary> revisionsMetadata = session.advanced().revisions().getMetadataFor(id);
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
                List<IMetadataDictionary> revisionsMetadata = session.advanced().revisions().getMetadataFor(id);
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
                List<IMetadataDictionary> revisionsMetadata = session.advanced().revisions().getMetadataFor(id);
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

            assertThatThrownBy(() -> store.maintenance().send(new ConfigureRevisionsOperation(configuration)))
                    .isInstanceOf(RavenException.class);
        }
    }

    @Test
    public void canGetNonExistingRevisionsByChangeVectorAsyncLazily() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Lazy<User> lazy = session.advanced().revisions().lazily().get(User.class, "dummy");
                User user = lazy.getValue();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(user)
                        .isNull();
            }
        }
    }

    @Test
    public void canGetRevisionsByChangeVectorsLazily() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "users/1";
            setupRevisions(store, false, 123);
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Omer");
                session.store(user, id);
                session.saveChanges();
            }

            for (int i = 0; i < 10; i++) {
                try (IDocumentSession session = store.openSession()) {
                    Company user = session.load(Company.class, id);
                    user.setName("Omer" + i);
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<IMetadataDictionary> revisionsMetadata = session.advanced().revisions().getMetadataFor(id);
                assertThat(revisionsMetadata)
                        .hasSize(11);

                String[] changeVectors = revisionsMetadata
                        .stream()
                        .map(x -> x.getString(Constants.Documents.Metadata.CHANGE_VECTOR))
                        .toArray(String[]::new);
                String[] changeVectors2 = revisionsMetadata
                        .stream()
                        .map(x -> x.getString(Constants.Documents.Metadata.CHANGE_VECTOR))
                        .toArray(String[]::new);

                Lazy<Map<String, User>> revisionsLazy = session.advanced().revisions().lazily().get(User.class, changeVectors);
                Lazy<Map<String, User>> revisionsLazy2 = session.advanced().revisions().lazily().get(User.class, changeVectors2);

                Map<String, User> lazyResult = revisionsLazy.getValue();
                Map<String, User> revisions = session.advanced().revisions().get(User.class, changeVectors);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
                assertThat(lazyResult.keySet())
                        .isEqualTo(revisions.keySet());
            }
        }
    }

    @Test
    public void canGetForLazily() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "users/1";
            String id2 = "users/2";

            setupRevisions(store, false, 123);

            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Omer");
                session.store(user1, id);

                User user2 = new User();
                user2.setName("Rhinos");
                session.store(user2, id2);

                session.saveChanges();
            }

            for (int i = 0; i < 10; i++) {
                try (IDocumentSession session = store.openSession()) {
                    Company user = session.load(Company.class, id);
                    user.setName("Omer" + i);
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<User> revision = session.advanced().revisions().getFor(User.class, "users/1");
                Lazy<List<User>> revisionsLazily = session.advanced().revisions().lazily().getFor(User.class, "users/1");
                session.advanced().revisions().lazily().getFor(User.class, "users/2");

                List<User> revisionsLazilyResult = revisionsLazily.getValue();

                assertThat(revision.stream().map(User::getName).collect(Collectors.joining(",")))
                        .isEqualTo(revisionsLazilyResult.stream().map(User::getName).collect(Collectors.joining(",")));
                assertThat(revision.stream().map(User::getId).collect(Collectors.joining(",")))
                        .isEqualTo(revisionsLazilyResult.stream().map(User::getId).collect(Collectors.joining(",")));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
            }
        }
    }

    @Test
    public void canGetRevisionsByIdAndTimeLazily() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "users/1";
            String id2 = "users/2";

            setupRevisions(store, false, 123);

            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Omer");
                session.store(user1, id);

                User user2 = new User();
                user2.setName("Rhinos");
                session.store(user2, id2);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Lazy<User> revision = session.advanced().lazily().load(User.class, "users/1");
                User doc = revision.getValue();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                User revision = session.advanced().revisions().get(User.class, "users/1", new Date());

                Lazy<User> revisionLazily = session.advanced().revisions().lazily().get(User.class, "users/1", new Date());
                session.advanced().revisions().lazily().get(User.class, "users/2", new Date());

                User revisionLazilyResult = revisionLazily.getValue();

                assertThat(revision.getId())
                        .isEqualTo(revisionLazilyResult.getId());
                assertThat(revisionLazilyResult.getName())
                        .isEqualTo(revisionLazilyResult.getName());
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
            }
        }
    }

    @Test
    public void canGetMetadataForLazily() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "users/1";
            String id2 = "users/2";

            setupRevisions(store, false, 123);

            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Omer");
                session.store(user1, id);

                User user2 = new User();
                user2.setName("Rhinos");
                session.store(user2, id2);

                session.saveChanges();
            }

            for (int i = 0; i < 10; i++) {
                try (IDocumentSession session = store.openSession()) {
                    Company user = session.load(Company.class, id);
                    user.setName("Omer" + i);
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<IMetadataDictionary> revisionsMetadata = session.advanced().revisions().getMetadataFor(id);
                Lazy<List<MetadataAsDictionary>> revisionsMetaDataLazily = session.advanced().revisions().lazily().getMetadataFor(id);
                Lazy<List<MetadataAsDictionary>> revisionsMetaDataLazily2 = session.advanced().revisions().lazily().getMetadataFor(id2);
                List<MetadataAsDictionary> revisionsMetaDataLazilyResult = revisionsMetaDataLazily.getValue();

                assertThat(revisionsMetadata.stream().map(x -> x.getString("@id")).collect(Collectors.joining(",")))
                        .isEqualTo(revisionsMetaDataLazilyResult.stream().map(x -> x.getString("@id")).collect(Collectors.joining(",")));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
            }
        }
    }

    @Test
    public void canGetRevisionsByChangeVectorLazily() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {


            String id = "users/1";
            String id2 = "users/2";

            setupRevisions(store, false, 123);

            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Omer");
                session.store(user1, id);

                User user2 = new User();
                user2.setName("Rhinos");
                session.store(user2, id2);

                session.saveChanges();
            }

            for (int i = 0; i < 10; i++) {
                try (IDocumentSession session = store.openSession()) {
                    Company user = session.load(Company.class, id);
                    user.setName("Omer" + i);
                    session.saveChanges();
                }
            }

            DatabaseStatistics stats = store.maintenance().send(new GetStatisticsOperation());
            String dbId = stats.getDatabaseId();

            String cv = "A:23-" + dbId;
            String cv2 = "A:3-" + dbId;

            try (IDocumentSession session = store.openSession()) {
                User revisions = session.advanced().revisions().get(User.class, cv);
                Lazy<User> revisionsLazily = session.advanced().revisions().lazily().get(User.class, cv);
                Lazy<User> revisionsLazily1 = session.advanced().revisions().lazily().get(User.class, cv2);

                User revisionsLazilyValue = revisionsLazily.getValue();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(revisionsLazilyValue.getId())
                        .isEqualTo(revisions.getId());
                assertThat(revisionsLazilyValue.getName())
                        .isEqualTo(revisions.getName());
            }

            try (IDocumentSession session = store.openSession()) {
                User revisions = session.advanced().revisions().get(User.class, cv);
                Lazy<User> revisionsLazily = session.advanced().revisions().lazily().get(User.class, cv);
                User revisionsLazilyValue = revisionsLazily.getValue();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(revisionsLazilyValue.getId())
                        .isEqualTo(revisions.getId());
                assertThat(revisionsLazilyValue.getName())
                        .isEqualTo(revisions.getName());
            }
        }
    }

    @Test
    public void canGetAllRevisionsForDocument_UsingStoreOperation() throws Exception {
        Company company = new Company();
        company.setName("Company Name");
        try (IDocumentStore store = getDocumentStore()) {
            setupRevisions(store, false, 123);

            try (IDocumentSession session = store.openSession()) {
                session.store(company);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company3 = session.load(Company.class, company.getId());
                company3.setName("Hibernating Rhinos");
                session.saveChanges();
            }

            RevisionsResult<Company> revisionsResult = store.operations().send(new GetRevisionsOperation<>(Company.class, company.getId()));

            assertThat(revisionsResult.getTotalResults())
                    .isEqualTo(2);

            List<Company> companiesRevisions = revisionsResult.getResults();
            assertThat(companiesRevisions)
                    .hasSize(2);
            assertThat(companiesRevisions.get(0).getName())
                    .isEqualTo("Hibernating Rhinos");
            assertThat(companiesRevisions.get(1).getName())
                    .isEqualTo("Company Name");
        }
    }

    @Test
    public void canGetRevisionsWithPaging_UsingStoreOperation() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setupRevisions(store, false, 123);

            String id = "companies/1";

            try (IDocumentSession session = store.openSession()) {
                session.store(new Company(), id);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company2 = session.load(Company.class, id);
                company2.setName("Hibernating");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company3 = session.load(Company.class, id);
                company3.setName("Hibernating Rhinos");
                session.saveChanges();
            }

            for (int i = 0; i < 10; i++) {
                try (IDocumentSession session = store.openSession()) {
                    Company company = session.load(Company.class, id);
                    company.setName("HR" + i);
                    session.saveChanges();
                }
            }

            GetRevisionsOperation.Parameters parameters = new GetRevisionsOperation.Parameters();
            parameters.setId(id);
            parameters.setStart(10);
            RevisionsResult<Company> revisionsResult = store.operations().send(new GetRevisionsOperation<>(Company.class, parameters));

            assertThat(revisionsResult.getTotalResults())
                    .isEqualTo(13);

            List<Company> companiesRevisions = revisionsResult.getResults();
            assertThat(companiesRevisions)
                    .hasSize(3);

            assertThat(companiesRevisions.get(0).getName())
                    .isEqualTo("Hibernating Rhinos");
            assertThat(companiesRevisions.get(1).getName())
                    .isEqualTo("Hibernating");
            assertThat(companiesRevisions.get(2).getName())
                    .isNull();
        }
    }

    @Test
    public void canGetRevisionsWithPaging2_UsingStoreOperation() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setupRevisions(store, false, 100);

            String id = "companies/1";

            try (IDocumentSession session = store.openSession()) {
                session.store(new Company(), id);
                session.saveChanges();
            }

            for (int i = 0; i < 99; i++) {
                try (IDocumentSession session = store.openSession()) {
                    Company company = session.load(Company.class, id);
                    company.setName("HR" + i);
                    session.saveChanges();
                }
            }

            RevisionsResult<Company> revisionsResult = store.operations().send(new GetRevisionsOperation<>(Company.class, id, 50, 10));

            assertThat(revisionsResult.getTotalResults())
                    .isEqualTo(100);

            List<Company> companiesRevisions = revisionsResult.getResults();
            assertThat(companiesRevisions)
                    .hasSize(10);

            int count = 0;
            for (int i = 48; i > 38; i--) {
                assertThat(companiesRevisions.get(count++).getName())
                        .isEqualTo("HR" + i);
            }
        }
    }

    @Test
    public void canGetRevisionsCountFor() throws Exception {
        Company company = new Company();
        company.setName("Company Name");

        try (IDocumentStore store = getDocumentStore()) {
            setupRevisions(store, false, 100);

            try (IDocumentSession session = store.openSession()) {
                session.store(company);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company2 = session.load(Company.class, company.getId());
                company2.setAddress1("Israel");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company3 = session.load(Company.class, company.getId());
                company3.setName("Hibernating Rhinos");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                long companiesRevisionsCount = session.advanced().revisions().getCountFor(company.getId());
                assertThat(companiesRevisionsCount)
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void canGetRevisionsByChangeVectors2() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            setupRevisions(store, false, 5);

            User user1 = new User();
            user1.setName("Jane");

            try (IDocumentSession session = store.openSession()) {
                session.store(user1);
                session.saveChanges();

                for (int i = 0; i < 10; i++) {
                    User user = session.load(User.class, user1.getId());
                    user.setName("Jane" + i);
                    session.saveChanges();
                }
            }

            String[] allCVS;

            try (IDocumentSession session = store.openSession()) {
                List<IMetadataDictionary> metadata = session.advanced().revisions().getMetadataFor(user1.getId());
                allCVS = metadata.stream().map(x -> x.get(Constants.Documents.Metadata.CHANGE_VECTOR).toString()).toArray(String[]::new);

                Map<String, User> revisionsByCv = session.advanced().revisions().get(User.class, allCVS);
                for (int i = 0; i < allCVS.length; i++) {
                    assertThat(revisionsByCv)
                            .containsKey(allCVS[i]);

                    User user = session.advanced().revisions().get(User.class, allCVS[i]);
                    assertThat(revisionsByCv.get(allCVS[i]).getName())
                            .isEqualTo(user.getName());
                }
            }
        }
    }

    @Test
    public void canGetRevisionsByChangeVectorsNonExist() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            setupRevisions(store, false, 5);

            User user1 = new User();
            user1.setName("Jane");

            try (IDocumentSession session = store.openSession()) {
                session.store(user1);
                session.saveChanges();

                for (int i = 0; i < 10; i++) {
                    User user = session.load(User.class, user1.getId());
                    user.setName("Jane" + i);
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                String[] allCvs = new String[] { "AB:1", "AB:2", "AB:3"};
                Map<String, User> revisionsByCV = session.advanced().revisions().get(User.class, allCvs);

                assertThat(revisionsByCV)
                        .hasSize(3);

                for (User val : revisionsByCV.values()) {
                    assertThat(val)
                            .isNull();
                }

                String cv = "AB:1";
                User revision = session.advanced().revisions().get(User.class, cv);
                assertThat(revision)
                        .isNull();
            }
        }
    }

    @Test
    public void canGetRevisionsByIdStartTake() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            setupRevisions(store, false, 123);

            User user1 = new User();
            user1.setName("Jane");

            try (IDocumentSession session = store.openSession()) {
                session.store(user1);
                session.saveChanges();

                for (int i = 0; i < 10; i++) {
                    User user = session.load(User.class, user1.getId());
                    user.setName("Jane" + i);
                    session.saveChanges();
                }
            }

            RevisionsResult<User> revisionsResult = store.operations().send(new GetRevisionsOperation<>(User.class, user1.getId(), 0, 5));
            assertThat(revisionsResult.getTotalResults())
                    .isEqualTo(11);
            assertThat(revisionsResult.getResults())
                    .hasSize(5);

            Set<String> revisionNames = revisionsResult.getResults().stream().map(User::getName).collect(Collectors.toSet());

            for (int i = revisionsResult.getTotalResults() - 2; i >= 5; i--) {
                assertThat(revisionNames)
                        .contains("Jane" + i);
            }

            revisionsResult = store.operations().send(new GetRevisionsOperation<>(User.class, user1.getId(), 6, 5));
            assertThat(revisionsResult.getTotalResults())
                    .isEqualTo(11);
            assertThat(revisionsResult.getResults())
                    .hasSize(5);


            revisionNames = revisionsResult.getResults().stream().map(User::getName).collect(Collectors.toSet());

            for (int i = revisionsResult.getTotalResults() - 2 - 6; i >= 0; i--) {
                assertThat(revisionNames)
                        .contains("Jane" + i);
            }

        }
    }
}
