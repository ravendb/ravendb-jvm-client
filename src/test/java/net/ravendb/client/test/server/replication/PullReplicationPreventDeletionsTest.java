package net.ravendb.client.test.server.replication;

import net.ravendb.client.Constants;
import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.operations.expiration.ConfigureExpirationOperation;
import net.ravendb.client.documents.operations.expiration.ExpirationConfiguration;
import net.ravendb.client.documents.operations.replication.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.GenerateCertificateOperation;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class PullReplicationPreventDeletionsTest extends ReplicationTestBase {

    @Test
    public void preventDeletionsOnHub() throws Exception {
        try (DocumentStore hubStore = getSecuredDocumentStore()) {
            try (DocumentStore sinkStore = getSecuredDocumentStore()) {

                GenerateCertificateOperation.PullReplicationCertificate certificate
                        = hubStore.maintenance().send(new GenerateCertificateOperation());

                setupExpiration(sinkStore);

                PullReplicationDefinition pullReplicationDefinition = new PullReplicationDefinition();
                pullReplicationDefinition.setName("pullRepHub");
                pullReplicationDefinition.setMode(EnumSet.of(PullReplicationMode.HUB_TO_SINK, PullReplicationMode.SINK_TO_HUB));
                pullReplicationDefinition.setPreventDeletionsMode(PreventDeletionsMode.PREVENT_SINK_TO_HUB_DELETIONS);

                hubStore.maintenance().send(new PutPullReplicationAsHubOperation(pullReplicationDefinition));

                ReplicationHubAccess replicationHubAccess = new ReplicationHubAccess();
                replicationHubAccess.setName("hubAccess");
                replicationHubAccess.setCertificateBase64(certificate.getPublicKey());
                hubStore.maintenance().send(new RegisterReplicationHubAccessOperation("pullRepHub", replicationHubAccess));

                RavenConnectionString ravenConnectionString = new RavenConnectionString();
                ravenConnectionString.setDatabase(hubStore.getDatabase());
                ravenConnectionString.setName(hubStore.getDatabase() + "ConStr");
                ravenConnectionString.setTopologyDiscoveryUrls(hubStore.getUrls());

                sinkStore.maintenance().send(new PutConnectionStringOperation<>(ravenConnectionString));

                PullReplicationAsSink pullReplicationAsSink = new PullReplicationAsSink();
                pullReplicationAsSink.setConnectionStringName(hubStore.getDatabase() + "ConStr");
                pullReplicationAsSink.setCertificateWithPrivateKey(certificate.getCertificate());
                pullReplicationAsSink.setHubName("pullRepHub");
                pullReplicationAsSink.setMode(EnumSet.of(PullReplicationMode.HUB_TO_SINK, PullReplicationMode.SINK_TO_HUB));

                sinkStore.maintenance().send(new UpdatePullReplicationAsSinkOperation(pullReplicationAsSink));

                try (IDocumentSession s = sinkStore.openSession()) {
                    User user1 = new User();
                    user1.setSource("Sink");
                    s.store(user1, "users/insink/1");
                    s.advanced().getMetadataFor(user1).put(Constants.Documents.Metadata.EXPIRES, DateUtils.addMinutes(new Date(), 10));

                    User user2 = new User();
                    user2.setSource("Sink");
                    s.store(user2, "users/insink/2");
                    s.advanced().getMetadataFor(user2).put(Constants.Documents.Metadata.EXPIRES, DateUtils.addMinutes(new Date(), 10));

                    s.saveChanges();
                }

                try (IDocumentSession s = hubStore.openSession()) {
                    User u = new User();
                    u.setSource("Hub");
                    s.store(u, "users/inhub/1");
                    s.saveChanges();
                }

                assertThat(waitForDocument(User.class, sinkStore, "users/inhub/1"))
                        .isTrue();

                //make sure hub got both docs and expires gets deleted

                try (IDocumentSession h = hubStore.openSession()) {
                    // check hub got both docs
                    User doc1 = h.load(User.class, "users/insink/1");
                    assertThat(doc1)
                            .isNotNull();

                    User doc2 = h.load(User.class, "users/insink/2");
                    assertThat(doc2)
                            .isNotNull();

                    //check expired does not exist in users/insink/1
                    IMetadataDictionary metadata = h.advanced().getMetadataFor(doc1);
                    assertThat(metadata.containsKey(Constants.Documents.Metadata.EXPIRES))
                            .isFalse();

                    //check expired does not exist in users/insink/2
                    metadata = h.advanced().getMetadataFor(doc2);
                    assertThat(metadata.containsKey(Constants.Documents.Metadata.EXPIRES))
                            .isFalse();
                }

                // delete doc from sink
                try (IDocumentSession s = sinkStore.openSession()) {
                    s.delete("users/insink/1");
                    s.saveChanges();
                }

                ensureReplicating(hubStore, sinkStore);

                //make sure doc is deleted from sink
                assertThat(waitForDocumentDeletion(sinkStore, "users/insink/1"))
                        .isTrue();

                //make sure doc not deleted from hub and still doesn't contain expires
                try (IDocumentSession h = hubStore.openSession()) {

                    //check hub got doc
                    User doc1 = h.load(User.class, "users/insink/1");
                    assertThat(doc1)
                            .isNotNull();

                    //check expires does not exist in users/insink/1
                    IMetadataDictionary metadata = h.advanced().getMetadataFor(doc1);
                    assertThat(metadata.containsKey(Constants.Documents.Metadata.EXPIRES))
                            .isFalse();
                }
            }
        }
    }

    private void setupExpiration(DocumentStore store) throws InterruptedException {
        ExpirationConfiguration config = new ExpirationConfiguration();
        config.setDisabled(false);
        config.setDeleteFrequencyInSec(2L);

        store.maintenance().send(new ConfigureExpirationOperation(config));

        Thread.sleep(1_500);
    }

    public static class User {
        private String id;
        private String source;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
