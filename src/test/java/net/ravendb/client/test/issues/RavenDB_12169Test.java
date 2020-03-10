package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.batches.BatchPatchCommandData;
import net.ravendb.client.documents.operations.PatchRequest;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_12169Test extends RemoteTestBase {

    @Test
    public void canUseBatchPatchCommand() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                company1.setId("companies/1");
                company1.setName("C1");
                session.store(company1);

                Company company2 = new Company();
                company2.setId("companies/2");
                company2.setName("C2");
                session.store(company2);

                Company company3 = new Company();
                company3.setId("companies/3");
                company3.setName("C3");
                session.store(company3);

                Company company4 = new Company();
                company4.setId("companies/4");
                company4.setName("C4");
                session.store(company4);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company c1 = session.load(Company.class, "companies/1");
                Company c2 = session.load(Company.class, "companies/2");
                Company c3 = session.load(Company.class, "companies/3");
                Company c4 = session.load(Company.class, "companies/4");

                assertThat(c1.getName())
                        .isEqualTo("C1");
                assertThat(c2.getName())
                        .isEqualTo("C2");
                assertThat(c3.getName())
                        .isEqualTo("C3");
                assertThat(c4.getName())
                        .isEqualTo("C4");

                String[] ids = {c1.getId(), c3.getId()};

                session.advanced().defer(new BatchPatchCommandData(
                        PatchRequest.forScript("this.name = 'test'; "), null, ids));

                session.advanced().defer(new BatchPatchCommandData(
                        PatchRequest.forScript("this.name = 'test2'; "), null, c4.getId()));

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Company c1 = session.load(Company.class, "companies/1");
                Company c2 = session.load(Company.class, "companies/2");
                Company c3 = session.load(Company.class, "companies/3");
                Company c4 = session.load(Company.class, "companies/4");

                assertThat(c1.getName())
                        .isEqualTo("test");
                assertThat(c2.getName())
                        .isEqualTo("C2");
                assertThat(c3.getName())
                        .isEqualTo("test");
                assertThat(c4.getName())
                        .isEqualTo("test2");
            }

            try (IDocumentSession session = store.openSession()) {
                Company c2 = session.load(Company.class, "companies/2");

                session.advanced().defer(new BatchPatchCommandData(PatchRequest.forScript("this.name = 'test2'"),
                        null, BatchPatchCommandData.IdAndChangeVector.create(c2.getId(),
                        "invalidCV")));

                assertThatThrownBy(() -> {
                    session.saveChanges();
                }).isExactlyInstanceOf(ConcurrencyException.class);
            }

            try (IDocumentSession session = store.openSession()) {
                Company c1 = session.load(Company.class, "companies/1");
                Company c2 = session.load(Company.class, "companies/2");
                Company c3 = session.load(Company.class, "companies/3");
                Company c4 = session.load(Company.class, "companies/4");

                assertThat(c1.getName())
                        .isEqualTo("test");
                assertThat(c2.getName())
                        .isEqualTo("C2");
                assertThat(c3.getName())
                        .isEqualTo("test");
                assertThat(c4.getName())
                        .isEqualTo("test2");
            }
        }
    }
}
