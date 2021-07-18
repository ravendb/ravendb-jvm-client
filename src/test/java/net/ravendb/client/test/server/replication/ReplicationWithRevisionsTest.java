package net.ravendb.client.test.server.replication;

import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.operations.ConfigureRevisionsForConflictsOperation;
import net.ravendb.client.serverwide.operations.ConfigureRevisionsForConflictsResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class ReplicationWithRevisionsTest extends ReplicationTestBase {

    @Test
    public void canDisableRevisionsConflict() throws Exception {
        try (IDocumentStore storeA = getDocumentStore()) {
            try (IDocumentStore storeB = getDocumentStore()) {

                RevisionsCollectionConfiguration collectionConfiguration = new RevisionsCollectionConfiguration();
                collectionConfiguration.setDisabled(true);
                ConfigureRevisionsForConflictsOperation conflictsOperation
                        = new ConfigureRevisionsForConflictsOperation(storeB.getDatabase(), collectionConfiguration);
                ConfigureRevisionsForConflictsResult result = storeB.maintenance().server().send(conflictsOperation);

                assertThat(result.getRaftCommandIndex())
                        .isPositive();

                try (IDocumentSession session = storeB.openSession()) {
                    session.store(new Company(), "keep-conflicted-revision-insert-order");
                    session.saveChanges();

                    User karmel = new User();
                    karmel.setName("Karmel-A-1");

                    session.store(karmel, "foo/bar");
                    session.saveChanges();
                }

                try (IDocumentSession session = storeA.openSession()) {
                    User user = new User();
                    user.setName("Karmel-B-1");
                    session.store(user);
                    session.saveChanges();
                }

                setupReplication(storeA, storeB);
                ensureReplicating(storeA, storeB);

                try (IDocumentSession session = storeB.openSession()) {
                    assertThat(session.advanced().revisions().getMetadataFor("foo/bar").size())
                            .isZero();
                }
            }
        }
    }
}
