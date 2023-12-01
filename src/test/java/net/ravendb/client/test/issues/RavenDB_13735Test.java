package net.ravendb.client.test.issues;

import com.google.common.base.Stopwatch;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.refresh.ConfigureRefreshOperation;
import net.ravendb.client.documents.operations.refresh.RefreshConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.TimeoutException;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.NetISO8601Utils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class RavenDB_13735Test extends RemoteTestBase {

    private void setupRefresh(IDocumentStore store) {
        RefreshConfiguration config = new RefreshConfiguration();
        config.setDisabled(false);
        config.setRefreshFrequencyInSec(1L);

        store.maintenance().send(new ConfigureRefreshOperation(config));
    }

    @Test
    public void refreshWillUpdateDocumentChangeVector() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setupRefresh(store);

            String expectedChangeVector = null;
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/1-A");

                Date hourAgo = DateUtils.addHours(new Date(), -1);
                session.advanced().getMetadataFor(user).put("@refresh", NetISO8601Utils.format(hourAgo, true));

                session.saveChanges();

                expectedChangeVector = session.advanced().getChangeVectorFor(user);
            }

            Stopwatch sw = Stopwatch.createStarted();

            while (true) {
                if (sw.elapsed(TimeUnit.SECONDS) > 10) {
                    throw new TimeoutException();
                }

                try (IDocumentSession session = store.openSession()) {
                    User user = session.load(User.class, "users/1-A");
                    assertThat(user)
                            .isNotNull();

                    if (!session.advanced().getChangeVectorFor(user).equals(expectedChangeVector)) {
                        // change vector was changed - great!
                        break;
                    }
                }

                Thread.sleep(200);
            }
        }
    }
}
