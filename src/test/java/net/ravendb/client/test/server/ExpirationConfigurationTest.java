package net.ravendb.client.test.server;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.expiration.ConfigureExpirationOperation;
import net.ravendb.client.documents.operations.expiration.ConfigureExpirationOperationResult;
import net.ravendb.client.documents.operations.expiration.ExpirationConfiguration;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class ExpirationConfigurationTest extends RemoteTestBase {

    @Test
    public void canSetupExpiration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            ExpirationConfiguration expirationConfiguration = new ExpirationConfiguration();
            expirationConfiguration.setDeleteFrequencyInSec(5L);
            expirationConfiguration.setDisabled(false);
            ConfigureExpirationOperation configureExpirationOperation = new ConfigureExpirationOperation(expirationConfiguration);

            ConfigureExpirationOperationResult expirationOperationResult =
                    store.maintenance().send(configureExpirationOperation);

            assertThat(expirationOperationResult.getRaftCommandIndex())
                    .isNotNull();
        }
    }
}
