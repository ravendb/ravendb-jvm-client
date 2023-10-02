package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.sorting.SorterDefinition;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.compilation.SorterCompilationException;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.orders.Company;
import net.ravendb.client.serverwide.operations.sorters.DeleteServerWideSorterOperation;
import net.ravendb.client.serverwide.operations.sorters.PutServerWideSortersOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisabledOnPullRequest
public class RavenDB_16328Test extends RemoteTestBase {

    @Test
    public void canUseCustomSorter() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company c1 = new Company();
                c1.setName("C1");

                Company c2 = new Company();
                c2.setName("C2");

                session.store(c1);
                session.store(c2);
                session.saveChanges();
            }

            String sorterName = store.getDatabase();

            String sorterCode = getSorter(sorterName);

            SorterDefinition sorterDefinition = new SorterDefinition();
            sorterDefinition.setName(sorterName);
            sorterDefinition.setCode(sorterCode);
            store.maintenance().server().send(new PutServerWideSortersOperation(sorterDefinition));

            // checking if we can send again same sorter
            store.maintenance().server().send(new PutServerWideSortersOperation(sorterDefinition));

            sorterCode = sorterCode.replaceAll("Catch me", "Catch me 2");

            // checking if we can update sorter
            SorterDefinition updatedSorter = new SorterDefinition();
            updatedSorter.setName(sorterName);
            updatedSorter.setCode(sorterCode);
            store.maintenance().server().send(new PutServerWideSortersOperation(updatedSorter));

            // We should not be able to add sorter with non-matching name
            String finalSorterCode = sorterCode;
            assertThatThrownBy(() -> {
                SorterDefinition invalidSorter = new SorterDefinition();
                invalidSorter.setName(sorterName + "_OtherName");
                invalidSorter.setCode(finalSorterCode);
                store.maintenance().server().send(new PutServerWideSortersOperation(invalidSorter));
            })
                    .isExactlyInstanceOf(SorterCompilationException.class);

            store.maintenance().server().send(new DeleteServerWideSorterOperation(sorterName));
        }
    }

    private static String getSorter(String sorterName) {
        return RavenDB_8355Test.sorterCode.replaceAll("MySorter", sorterName);
    }
}
