package net.ravendb.client.test.issues;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexResetMode;
import net.ravendb.client.documents.operations.indexes.GetIndexNamesOperation;
import net.ravendb.client.documents.operations.indexes.ResetIndexOperation;
import net.ravendb.client.documents.operations.indexes.StopIndexingOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_26938Test extends RemoteTestBase {

    public static class DocIndex extends AbstractIndexCreationTask {
        public DocIndex() {
            map = "from u in docs.Users select new { u.Name }";
        }
    }

    public static class OtherIndex extends AbstractIndexCreationTask {
        public OtherIndex() {
            map = "from u in docs.Users select new { u.LastName }";
        }
    }

    @Test
    public void sideBySideReplacementFromResetShouldSurviveDatabaseRecordChange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            DocIndex index = new DocIndex();
            index.execute(store);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("value");
                session.store(user, "users/1");
                session.saveChanges();
            }

            waitForIndexing(store);

            // stop indexing so the side-by-side replacement cannot complete and swap
            store.maintenance().send(new StopIndexingOperation());
            store.maintenance().send(new ResetIndexOperation(index.getIndexName(), IndexResetMode.SIDE_BY_SIDE));

            String replacementName = Constants.Documents.Indexing.SIDE_BY_SIDE_INDEX_NAME_PREFIX + index.getIndexName();

            assertThat(store.maintenance().send(new GetIndexNamesOperation(0, 100))).contains(replacementName);

            // any database record change; here: deploying an unrelated index
            new OtherIndex().execute(store);

            // the replacement should still be there
            assertThat(store.maintenance().send(new GetIndexNamesOperation(0, 100))).contains(replacementName);
        }
    }
}
