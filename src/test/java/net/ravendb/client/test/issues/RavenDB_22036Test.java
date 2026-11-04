package net.ravendb.client.test.issues;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexResetMode;
import net.ravendb.client.documents.operations.indexes.GetIndexNamesOperation;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.operations.indexes.ResetIndexOperation;
import net.ravendb.client.documents.operations.indexes.StartIndexingOperation;
import net.ravendb.client.documents.operations.indexes.StopIndexingOperation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_22036Test extends RemoteTestBase {

    private static final String INDEX_NAME = "Users/ByName";
    private static final String REPLACEMENT_NAME = Constants.Documents.Indexing.SIDE_BY_SIDE_INDEX_NAME_PREFIX + INDEX_NAME;

    private static void putIndexAndStopIndexing(IDocumentStore store) {
        IndexDefinition definition = new IndexDefinition();
        definition.setName(INDEX_NAME);
        definition.setMaps(Collections.singleton("from user in docs.Users select new { user.FirstName }"));
        store.maintenance().send(new PutIndexesOperation(definition));

        store.maintenance().send(new StopIndexingOperation());
    }

    private static List<String> indexNames(IDocumentStore store) {
        return Arrays.asList(store.maintenance().send(new GetIndexNamesOperation(0, Integer.MAX_VALUE)));
    }

    private static void resumeIndexingAndAssertOnlyTheOriginalIsLeft(IDocumentStore store) {
        store.maintenance().send(new StartIndexingOperation());
        waitForIndexing(store);

        assertThat(indexNames(store)).containsExactly(INDEX_NAME);
    }

    @Test
    public void sideBySideIndexIsCreatedOnResetSideBySide() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putIndexAndStopIndexing(store);

            store.maintenance().send(new ResetIndexOperation(INDEX_NAME, IndexResetMode.SIDE_BY_SIDE));

            assertThat(indexNames(store)).containsExactlyInAnyOrder(INDEX_NAME, REPLACEMENT_NAME);

            resumeIndexingAndAssertOnlyTheOriginalIsLeft(store);
        }
    }

    @Test
    public void sideBySideIndexIsNotCreatedOnResetInPlace() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putIndexAndStopIndexing(store);

            store.maintenance().send(new ResetIndexOperation(INDEX_NAME, IndexResetMode.IN_PLACE));

            assertThat(indexNames(store)).containsExactly(INDEX_NAME);

            resumeIndexingAndAssertOnlyTheOriginalIsLeft(store);
        }
    }

    @Test
    public void consecutiveSideBySideResets() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putIndexAndStopIndexing(store);

            store.maintenance().send(new ResetIndexOperation(INDEX_NAME, IndexResetMode.SIDE_BY_SIDE));
            assertThat(indexNames(store)).containsExactlyInAnyOrder(INDEX_NAME, REPLACEMENT_NAME);

            store.maintenance().send(new ResetIndexOperation(INDEX_NAME, IndexResetMode.SIDE_BY_SIDE));
            assertThat(indexNames(store)).containsExactlyInAnyOrder(INDEX_NAME, REPLACEMENT_NAME);

            resumeIndexingAndAssertOnlyTheOriginalIsLeft(store);
        }
    }

    @Test
    public void resetIndexOperationWithoutIndexResetModeResetsInPlace() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putIndexAndStopIndexing(store);

            store.maintenance().send(new ResetIndexOperation(INDEX_NAME));

            assertThat(indexNames(store)).containsExactly(INDEX_NAME);

            resumeIndexingAndAssertOnlyTheOriginalIsLeft(store);
        }
    }

    @Test
    public void inPlaceResetOnIndexWithRunningSideBySide() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putIndexAndStopIndexing(store);

            store.maintenance().send(new ResetIndexOperation(INDEX_NAME, IndexResetMode.SIDE_BY_SIDE));
            assertThat(indexNames(store)).containsExactlyInAnyOrder(INDEX_NAME, REPLACEMENT_NAME);

            store.maintenance().send(new ResetIndexOperation(INDEX_NAME));
            assertThat(indexNames(store)).containsExactlyInAnyOrder(INDEX_NAME, REPLACEMENT_NAME);

            resumeIndexingAndAssertOnlyTheOriginalIsLeft(store);
        }
    }
}
