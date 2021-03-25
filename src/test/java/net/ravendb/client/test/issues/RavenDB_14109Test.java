package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.primitives.Reference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14109Test extends RemoteTestBase {

    @Test
    public void queryStatsShouldBeFilledBeforeCallingMoveNext() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new Company());
                session.store(new Company());

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Company> query = session.query(Company.class);

                Reference<StreamQueryStatistics> statisticsReference = new Reference<>();
                try (CloseableIterator<StreamResult<Company>> iterator =
                        session.advanced().stream(query, statisticsReference)) {
                    assertThat(statisticsReference.value.getTotalResults())
                            .isEqualTo(2);

                    int count = 0;
                    while (iterator.hasNext()) {
                        iterator.next();
                        count++;
                    }

                    assertThat(count)
                            .isEqualTo(statisticsReference.value.getTotalResults());
                }
            }
        }
    }
}
