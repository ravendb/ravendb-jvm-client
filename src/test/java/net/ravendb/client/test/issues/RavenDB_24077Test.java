package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexCreation;
import net.ravendb.client.documents.operations.indexes.GetIndexNamesOperation;
import net.ravendb.client.exceptions.documents.compilation.IndexCompilationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_24077Test extends RemoteTestBase {

    @Test
    public void createIndexes_Should_Not_Throw_When_Indexes_List_Is_Empty() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexCreation.createIndexes(Collections.emptyList(), store);

            store.executeIndexes(new ArrayList<>());

            store.executeIndexes(new ArrayList<>(), null, null);
        }
    }

    @Test
    public void createIndexes_Does_Not_Fall_Back_To_One_By_One_Creation() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> IndexCreation.createIndexes(Arrays.asList(new Valid_Index(), new Invalid_Index()), store))
                    .isInstanceOf(IndexCompilationException.class);

            // indexes are put in one batch, so the valid index must not be created on its own
            String[] indexNames = store.maintenance().send(new GetIndexNamesOperation(0, 10));
            assertThat(indexNames).isEmpty();
        }
    }

    public static class Valid_Index extends AbstractIndexCreationTask {
        public Valid_Index() {
            map = "from u in docs.Users select new { u.name }";
        }
    }

    public static class Invalid_Index extends AbstractIndexCreationTask {
        public Invalid_Index() {
            map = "from u in docs.Users select new { u.name ";
        }
    }
}
