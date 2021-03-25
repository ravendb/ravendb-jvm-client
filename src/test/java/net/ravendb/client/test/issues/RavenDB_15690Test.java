package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.orders.Company;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15690Test extends RemoteTestBase {

    @Test
    public void hasChanges_ShouldDetectDeletes() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");
                session.delete(company);

                Map<String, List<DocumentsChanges>> changes = session.advanced().whatChanged();
                assertThat(changes)
                        .hasSize(1);
                assertThat(session.advanced().hasChanges())
                        .isTrue();
            }
        }
    }
}
