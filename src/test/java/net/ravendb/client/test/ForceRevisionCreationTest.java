package net.ravendb.client.test;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.session.ForceRevisionStrategy;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisabledOnPullRequest
public class ForceRevisionCreationTest extends RemoteTestBase {

    @Test
    public void forceRevisionCreationForSingleUnTrackedEntityByID() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String companyId;

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company);

                companyId = company.getId();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().revisions().forceRevisionCreationFor(companyId);
                session.saveChanges();

                int revisionsCount = session.advanced().revisions().getFor(Company.class, companyId).size();
                assertThat(revisionsCount)
                        .isOne();
            }
        }
    }

    @Test
    public void forceRevisionCreationForMultipleUnTrackedEntitiesByID() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String companyId1;
            String companyId2;

            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                company1.setName("HR1");

                Company company2 = new Company();
                company2.setName("HR2");

                session.store(company1);
                session.store(company2);

                companyId1 = company1.getId();
                companyId2 = company2.getId();

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().revisions().forceRevisionCreationFor(companyId1);
                session.advanced().revisions().forceRevisionCreationFor(companyId2);

                session.saveChanges();

                int revisionsCount1 = session.advanced().revisions().getFor(Company.class, companyId1).size();
                int revisionsCount2 = session.advanced().revisions().getFor(Company.class, companyId2).size();

                assertThat(revisionsCount1)
                        .isOne();

                assertThat(revisionsCount2)
                        .isOne();
            }
        }
    }

    @Test
    public void cannotForceRevisionCreationForUnTrackedEntityByEntity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");

                assertThatThrownBy(() -> session.advanced().revisions().forceRevisionCreationFor(company))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot create a revision for the requested entity because it is Not tracked by the session");
            }
        }
    }

    @Test
    public void forceRevisionCreationForNewDocumentByEntity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company);
                session.saveChanges();

                session.advanced().revisions().forceRevisionCreationFor(company);

                int revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isZero();

                session.saveChanges();

                revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isOne();
            }
        }
    }

    @Test
    public void cannotForceRevisionCreationForNewDocumentBeforeSavingToServerByEntity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company);

                session.advanced().revisions().forceRevisionCreationFor(company);

                assertThatThrownBy(session::saveChanges)
                        .isInstanceOf(RavenException.class)
                        .hasMessageContaining("Can't force revision creation - the document was not saved on the server yet");

                int revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isZero();
            }
        }
    }

    @Test
    public void forceRevisionCreationForTrackedEntityWithNoChangesByEntity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String companyId = "";

            try (IDocumentSession session = store.openSession()) {
                // 1. store document
                Company company = new Company();
                company.setName("HR");
                session.store(company);
                session.saveChanges();

                companyId = company.getId();

                int revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isZero();
            }

            try (IDocumentSession session = store.openSession()) {
                // 2. Load & Save without making changes to the document
                Company company = session.load(Company.class, companyId);

                session.advanced().revisions().forceRevisionCreationFor(company);
                session.saveChanges();

                int revisionsCount = session.advanced().revisions().getFor(Company.class, companyId).size();
                assertThat(revisionsCount)
                        .isOne();
            }
        }
    }

    @Test
    public void forceRevisionCreationForTrackedEntityWithChangesByEntity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String companyId = "";

            // 1. Store document
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company);
                session.saveChanges();

                companyId = company.getId();

                int revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isZero();
            }

            // 2. Load, Make changes & Save
            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, companyId);
                company.setName("HR V2");

                session.advanced().revisions().forceRevisionCreationFor(company);
                session.saveChanges();

                List<Company> revisions = session.advanced().revisions().getFor(Company.class, company.getId());
                int revisionsCount = revisions.size();

                assertThat(revisionsCount)
                        .isOne();

                // Assert revision contains the value 'Before' the changes...
                // ('Before' is the default force revision creation strategy)
                assertThat(revisions.get(0).getName())
                        .isEqualTo("HR");
            }
        }
    }

    @Test
    public void forceRevisionCreationForTrackedEntityWithChangesByID() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String companyId = "";

            // 1. Store document
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company);
                session.saveChanges();

                companyId = company.getId();

                int revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isZero();
            }

            try (IDocumentSession session = store.openSession()) {
                // 2. Load, Make changes & Save
                Company company = session.load(Company.class, companyId);
                company.setName("HR V2");

                session.advanced().revisions().forceRevisionCreationFor(company.getId());
                session.saveChanges();

                List<Company> revisions = session.advanced().revisions().getFor(Company.class, company.getId());
                int revisionsCount = revisions.size();


                assertThat(revisionsCount)
                        .isOne();

                // Assert revision contains the value 'Before' the changes...
                assertThat(revisions.get(0).getName())
                        .isEqualTo("HR");
            }
        }
    }

    @Test
    public void forceRevisionCreationMultipleRequests() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String companyId = "";

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company);
                session.saveChanges();

                companyId = company.getId();

                int revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isZero();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().revisions().forceRevisionCreationFor(companyId);

                Company company = session.load(Company.class, companyId);
                company.setName("HR V2");

                session.advanced().revisions().forceRevisionCreationFor(company);
                // The above request should not throw - we ignore duplicate requests with SAME strategy

                assertThatThrownBy(() -> session.advanced().revisions().forceRevisionCreationFor(company.getId(), ForceRevisionStrategy.NONE)).isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("A request for creating a revision was already made for document");

                session.saveChanges();

                List<Company> revisions = session.advanced().revisions().getFor(Company.class, company.getId());
                int revisionsCount = revisions.size();

                assertThat(revisionsCount)
                        .isOne();

                assertThat(revisions.get(0).getName())
                        .isEqualTo("HR");
            }
        }
    }

    @Test
    public void forceRevisionCreationAcrossMultipleSessions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String companyId = "";

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");

                session.store(company);
                session.saveChanges();

                companyId = company.getId();
                int revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isZero();

                session.advanced().revisions().forceRevisionCreationFor(company);
                session.saveChanges();

                revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isOne();

                // Verify that another 'force' request will not create another revision
                session.advanced().revisions().forceRevisionCreationFor(company);
                session.saveChanges();

                revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isOne();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, companyId);
                company.setName("HR V2");

                session.advanced().revisions().forceRevisionCreationFor(company);
                session.saveChanges();

                List<Company> revisions = session.advanced().revisions().getFor(Company.class, company.getId());
                int revisionsCount = revisions.size();


                assertThat(revisionsCount)
                        .isOne();
                // Assert revision contains the value 'Before' the changes...
                assertThat(revisions.get(0).getName())
                        .isEqualTo("HR");

                session.advanced().revisions().forceRevisionCreationFor(company);
                session.saveChanges();

                revisions = session.advanced().revisions().getFor(Company.class, company.getId());
                revisionsCount = revisions.size();

                assertThat(revisionsCount)
                        .isEqualTo(2);

                // Assert revision contains the value 'Before' the changes...
                assertThat(revisions.get(0).getName())
                        .isEqualTo("HR V2");
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, companyId);
                company.setName("HR V3");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().revisions().forceRevisionCreationFor(companyId);
                session.saveChanges();

                List<Company> revisions = session.advanced().revisions().getFor(Company.class, companyId);
                int revisionsCount = revisions.size();

                assertThat(revisionsCount)
                        .isEqualTo(3);
                assertThat(revisions.get(0).getName())
                        .isEqualTo("HR V3");
            }

        }
    }

    @Test
    public void forceRevisionCreationWhenRevisionConfigurationIsSet() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            // Define revisions settings
            RevisionsConfiguration configuration = new RevisionsConfiguration();

            RevisionsCollectionConfiguration companiesConfiguration = new RevisionsCollectionConfiguration();
            companiesConfiguration.setPurgeOnDelete(true);
            companiesConfiguration.setMinimumRevisionsToKeep(5L);

            configuration.setCollections(Collections.singletonMap("Companies", companiesConfiguration));

            ConfigureRevisionsOperation.ConfigureRevisionsOperationResult result = store.maintenance().send(new ConfigureRevisionsOperation(configuration));
            String companyId = "";

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company);
                companyId = company.getId();
                session.saveChanges();

                int revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isOne(); // one revision because configuration is set

                session.advanced().revisions().forceRevisionCreationFor(company);
                session.saveChanges();

                revisionsCount = session.advanced().revisions().getFor(Company.class, company.getId()).size();
                assertThat(revisionsCount)
                        .isOne(); // no new revision created - already exists due to configuration settings

                session.advanced().revisions().forceRevisionCreationFor(company);
                session.saveChanges();

                company.setName("HR V2");
                session.saveChanges();

                List<Company> revisions = session.advanced().revisions().getFor(Company.class, companyId);
                revisionsCount = revisions.size();

                assertThat(revisionsCount)
                        .isEqualTo(2);
                assertThat(revisions.get(0).getName())
                        .isEqualTo("HR V2");
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().revisions().forceRevisionCreationFor(companyId);
                session.saveChanges();

                List<Company> revisions = session.advanced().revisions().getFor(Company.class, companyId);
                int revisionsCount = revisions.size();

                assertThat(revisionsCount)
                        .isEqualTo(2);

                assertThat(revisions.get(0).getName())
                        .isEqualTo("HR V2");
            }
        }
    }

    @Test
    public void hasRevisionsFlagIsCreatedWhenForcingRevisionForDocumentThatHasNoRevisionsYet() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String company1Id = "";
            String company2Id = "";

            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                company1.setName("HR1");

                Company company2 = new Company();
                company2.setName("HR2");

                session.store(company1);
                session.store(company2);

                session.saveChanges();

                company1Id = company1.getId();
                company2Id = company2.getId();

                int revisionsCount = session.advanced().revisions().getFor(Company.class, company1.getId()).size();
                assertThat(revisionsCount)
                        .isZero();

                revisionsCount = session.advanced().revisions().getFor(Company.class, company2.getId()).size();
                assertThat(revisionsCount)
                        .isZero();
            }

            try (IDocumentSession session = store.openSession()) {
                // Force revision with no changes on document
                session.advanced().revisions().forceRevisionCreationFor(company1Id);

                // Force revision with changes on document
                session.advanced().revisions().forceRevisionCreationFor(company2Id);
                Company company2 = session.load(Company.class, company2Id);
                company2.setName("HR2 New Name");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<Company> revisions = session.advanced().revisions().getFor(Company.class, company1Id);
                int revisionsCount = revisions.size();
                assertThat(revisionsCount)
                        .isOne();
                assertThat(revisions.get(0).getName())
                        .isEqualTo("HR1");

                revisions = session.advanced().revisions().getFor(Company.class, company2Id);
                revisionsCount = revisions.size();
                assertThat(revisionsCount)
                        .isOne();
                assertThat(revisions.get(0).getName())
                        .isEqualTo("HR2");

                // Assert that HasRevisions flag was created on both documents
                Company company = session.load(Company.class, company1Id);
                IMetadataDictionary metadata = session.advanced().getMetadataFor(company);
                assertThat(metadata.get("@flags"))
                        .isEqualTo("HasRevisions");

                company = session.load(Company.class, company2Id);
                metadata = session.advanced().getMetadataFor(company);
                assertThat(metadata.get("@flags"))
                        .isEqualTo("HasRevisions");
            }
        }
    }
}
