package net.ravendb.client.test.issues;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexErrors;
import net.ravendb.client.documents.operations.indexes.DeleteIndexErrorsOperation;
import net.ravendb.client.documents.operations.indexes.GetIndexErrorsOperation;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.indexes.IndexDoesNotExistException;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

public class RavenDB_6967Test extends RemoteTestBase {

    @Test
    public void canDeleteIndexErrors() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RavenTestHelper.assertNoIndexErrors(store);

            store.maintenance().send(new DeleteIndexErrorsOperation());

            assertThatThrownBy(() -> {
                store.maintenance().send(new DeleteIndexErrorsOperation(new String[] { "DoesNotExist" }));
            }).isInstanceOf(IndexDoesNotExistException.class);

            IndexDefinition index1 = new IndexDefinition();
            index1.setName("Index1");
            index1.setMaps(Collections.singleton("from doc in docs let x = 0 select new { Total = 3/x };"));

            store.maintenance().send(new PutIndexesOperation(index1));

            IndexDefinition index2 = new IndexDefinition();
            index2.setName("Index2");
            index2.setMaps(Collections.singleton("from doc in docs let x = 0 select new { Total = 4/x };"));

            store.maintenance().send(new PutIndexesOperation(index2));

            IndexDefinition index3 = new IndexDefinition();
            index3.setName("Index3");
            index3.setMaps(Collections.singleton("from doc in docs let x = 0 select new { Total = 5/x };"));

            store.maintenance().send(new PutIndexesOperation(index3));

            waitForIndexing(store);

            RavenTestHelper.assertNoIndexErrors(store);

            store.maintenance().send(new DeleteIndexErrorsOperation());

            store.maintenance().send(new DeleteIndexErrorsOperation(new String[]{ "Index1", "Index2", "Index3" }));

            assertThatThrownBy(() -> {
                store.maintenance().send(new DeleteIndexErrorsOperation(new String[] { "Index1", "DoesNotExist" }));
            }).isInstanceOf(IndexDoesNotExistException.class);

            try (IDocumentSession session = store.openSession()) {
                session.store(new Company());
                session.store(new Company());
                session.store(new Company());

                session.saveChanges();
            }

            waitForIndexingErrors(store, Duration.ofMinutes(1), "Index1");
            waitForIndexingErrors(store, Duration.ofMinutes(1), "Index2");
            waitForIndexingErrors(store, Duration.ofMinutes(1), "Index3");


            IndexErrors[] indexErrors1 = store.maintenance().send(new GetIndexErrorsOperation(new String[]{"Index1"}));
            IndexErrors[] indexErrors2 = store.maintenance().send(new GetIndexErrorsOperation(new String[]{"Index2"}));
            IndexErrors[] indexErrors3 = store.maintenance().send(new GetIndexErrorsOperation(new String[]{"Index3"}));

            assertThat(Arrays.stream(indexErrors1).flatMap(x -> Arrays.stream(x.getErrors())).count())
                    .isPositive();

            assertThat(Arrays.stream(indexErrors2).flatMap(x -> Arrays.stream(x.getErrors())).count())
                    .isPositive();

            assertThat(Arrays.stream(indexErrors3).flatMap(x -> Arrays.stream(x.getErrors())).count())
                    .isPositive();

            store.maintenance().send(new DeleteIndexErrorsOperation(new String[]{ "Index2" }));

            indexErrors1 = store.maintenance().send(new GetIndexErrorsOperation(new String[]{"Index1"}));
            indexErrors2 = store.maintenance().send(new GetIndexErrorsOperation(new String[]{"Index2"}));
            indexErrors3 = store.maintenance().send(new GetIndexErrorsOperation(new String[]{"Index3"}));

            assertThat(Arrays.stream(indexErrors1).flatMap(x -> Arrays.stream(x.getErrors())).count())
                    .isPositive();

            assertThat(Arrays.stream(indexErrors2).flatMap(x -> Arrays.stream(x.getErrors())).count())
                    .isZero();

            assertThat(Arrays.stream(indexErrors3).flatMap(x -> Arrays.stream(x.getErrors())).count())
                    .isPositive();

            store.maintenance().send(new DeleteIndexErrorsOperation());

            indexErrors1 = store.maintenance().send(new GetIndexErrorsOperation(new String[]{"Index1"}));
            indexErrors2 = store.maintenance().send(new GetIndexErrorsOperation(new String[]{"Index2"}));
            indexErrors3 = store.maintenance().send(new GetIndexErrorsOperation(new String[]{"Index3"}));

            assertThat(Arrays.stream(indexErrors1).flatMap(x -> Arrays.stream(x.getErrors())).count())
                    .isZero();

            assertThat(Arrays.stream(indexErrors2).flatMap(x -> Arrays.stream(x.getErrors())).count())
                    .isZero();

            assertThat(Arrays.stream(indexErrors3).flatMap(x -> Arrays.stream(x.getErrors())).count())
                    .isZero();

            RavenTestHelper.assertNoIndexErrors(store);
        }
    }
}
