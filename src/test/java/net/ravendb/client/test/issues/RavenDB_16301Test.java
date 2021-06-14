package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.queries.QueryData;
import net.ravendb.client.documents.session.ConditionalLoadResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_16301Test extends RemoteTestBase {

    @Test
    public void canUseConditionalLoadLazily() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                for (int i = 0; i < 100; i++) {
                    bulkInsert.store(new Company());
                }
            }

            List<Result> ids;
            List<Lazy<ConditionalLoadResult<Company>>> loads = new ArrayList<>();

            try (IDocumentSession session1 = store.openSession()) {
                ids = session1.advanced().documentQuery(Company.class)
                        .waitForNonStaleResults()
                        .selectFields(Result.class,
                                QueryData.customFunction("o",
                                        "{ id : id(o), changeVector : getMetadata(o)['@change-vector'] }"))
                        .toList();

                session1.load(Company.class,
                        ids
                                .stream()
                                .map(Result::getId)
                                .collect(Collectors.toList()));

                Map<String, Company> res =
                        session1.load(Company.class,
                                ids
                                        .stream()
                                        .limit(50)
                                        .map(Result::getId)
                                        .collect(Collectors.toList()));

                int c = 0;
                for (Map.Entry<String, Company> kvp : res.entrySet()) {
                    kvp.getValue().setPhone(++c);
                }

                session1.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                // load last 10
                session
                        .load(Company.class,
                                ids.stream().skip(90).limit(10).map(Result::getId).collect(Collectors.toList()));

                int numberOfRequestsPerSession = session.advanced().getNumberOfRequests();

                for (Result res : ids) {
                    loads.add(session.advanced().lazily().conditionalLoad(Company.class, res.getId(), res.getChangeVector()));
                }

                session.advanced().eagerly().executeAllPendingLazyOperations();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(numberOfRequestsPerSession + 1);

                for (int i = 0; i < 100; i++) {
                    Lazy<ConditionalLoadResult<Company>> l = loads.get(i);

                    assertThat(l.isValueCreated())
                            .isFalse();

                    ConditionalLoadResult<Company> conditionalLoadResult = l.getValue();

                    if (i < 50) {
                        // load from server
                        assertThat(conditionalLoadResult.getEntity().getId())
                                .isEqualTo(ids.get(i).getId());
                    } else if (i < 90) {
                        // not modified
                        assertThat(conditionalLoadResult.getEntity())
                                .isNull();
                        assertThat(conditionalLoadResult.getChangeVector())
                                .isEqualTo(ids.get(i).getChangeVector());
                    } else {
                        // tracked in session
                        assertThat(conditionalLoadResult.getEntity().getId())
                                .isEqualTo(ids.get(i).getId());
                        assertThat(conditionalLoadResult.getEntity())
                                .isNotNull();
                        assertThat(conditionalLoadResult.getChangeVector())
                                .isEqualTo(ids.get(i).getChangeVector());
                    }

                    // not exist on server
                    Lazy<ConditionalLoadResult<Company>> lazy = session.advanced().lazily().conditionalLoad(Company.class, "Companies/322-A", ids.get(0).getChangeVector());
                    ConditionalLoadResult<Company> load = lazy.getValue();
                    assertThat(load.getEntity())
                            .isNull();
                    assertThat(load.getChangeVector())
                            .isNull();
                }
            }
        }
    }

    public static class Result {
        private String id;
        private String changeVector;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getChangeVector() {
            return changeVector;
        }

        public void setChangeVector(String changeVector) {
            this.changeVector = changeVector;
        }
    }
}
