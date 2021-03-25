package net.ravendb.client.documents.commands;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CanGetBuildNumberTest extends RemoteTestBase {

    @Test
    public void canGetBuildNumber() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            BuildNumber buildNumber = store.maintenance().server().send(new GetBuildNumberOperation());
            assertThat(buildNumber)
                    .isNotNull();

            assertThat(buildNumber.getProductVersion())
                    .isNotNull();
        }
    }
}
