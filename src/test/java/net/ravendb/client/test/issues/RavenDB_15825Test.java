package net.ravendb.client.test.issues;

import net.ravendb.client.Parameters;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.queries.HashCalculator;
import net.ravendb.client.documents.queries.facets.FacetOptions;
import net.ravendb.client.documents.queries.facets.FacetResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.OrderingType;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.primitives.Reference;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15825Test extends RemoteTestBase {

    private static final String[] TAGS = new String[] { "test", "label", "vip", "apple", "orange" };

    @Test
    public void shouldWork() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new ContactsIndex().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Random random = new Random();
                for (int id = 0; id < 10000; id++) {
                    int companyId = id % 100;

                    Contact contact = new Contact();
                    contact.setId("contacts/" + id);
                    contact.setCompanyId(companyId);
                    contact.setActive(id % 2 == 0);
                    contact.setTags(new String[] { TAGS[id % TAGS.length] });

                    session.store(contact);
                }

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {

                Reference<QueryStatistics> statsRef = new Reference<>();
                Map<String, FacetResult> res = facet(session, 1, 3, statsRef);

                assertThat(statsRef.value.getDurationInMs())
                        .isNotEqualTo(-1);
                assertThat(res.get("companyId").getValues())
                        .hasSize(3);

                assertThat(res.get("companyId").getValues().get(0).getRange())
                        .isEqualTo("28");
                assertThat(res.get("companyId").getValues().get(1).getRange())
                        .isEqualTo("38");
                assertThat(res.get("companyId").getValues().get(2).getRange())
                        .isEqualTo("48");

                Reference<QueryStatistics> stats2Ref = new Reference<>();
                Map<String, FacetResult> res2 = facet(session, 2, 1, stats2Ref);
                assertThat(stats2Ref.value.getDurationInMs())
                        .isNotEqualTo(-1);
                assertThat(res2.get("companyId").getValues())
                        .hasSize(1);

                assertThat(res2.get("companyId").getValues().get(0).getRange())
                        .isEqualTo("38");

                Reference<QueryStatistics> stats3Ref = new Reference<>();
                Map<String, FacetResult> res3 = facet(session, 5, 5, stats3Ref);
                assertThat(stats3Ref.value.getDurationInMs())
                        .isNotEqualTo(-1);

                assertThat(res3.get("companyId").getValues())
                        .hasSize(5);
                assertThat(res3.get("companyId").getValues().get(0).getRange())
                        .isEqualTo("68");
                assertThat(res3.get("companyId").getValues().get(1).getRange())
                        .isEqualTo("78");
                assertThat(res3.get("companyId").getValues().get(2).getRange())
                        .isEqualTo("8");
                assertThat(res3.get("companyId").getValues().get(3).getRange())
                        .isEqualTo("88");
                assertThat(res3.get("companyId").getValues().get(4).getRange())
                        .isEqualTo("98");
            }
        }
    }

    @Test
    public void canHashCorrectly() throws Exception {
        FacetOptions facetOptions = new FacetOptions();
        facetOptions.setStart(1);
        facetOptions.setPageSize(5);

        Parameters p = new Parameters();
        p.put("p1", facetOptions);

        HashCalculator hashCalculator = new HashCalculator();
        hashCalculator.write(p, DocumentConventions.defaultConventions.getEntityMapper());
        String hash1 = hashCalculator.getHash();

        // create second object with same props
        FacetOptions facetOptions2 = new FacetOptions();
        facetOptions2.setStart(1);
        facetOptions2.setPageSize(5);

        Parameters p2 = new Parameters();
        p2.put("p1", facetOptions2);

        HashCalculator hashCalculator2 = new HashCalculator();
        hashCalculator2.write(p2, DocumentConventions.defaultConventions.getEntityMapper());
        String hash2 = hashCalculator2.getHash();

        // modify original object - it should change hash
        facetOptions.setStart(2);
        HashCalculator hashCalculator3 = new HashCalculator();
        hashCalculator3.write(p, DocumentConventions.defaultConventions.getEntityMapper());
        String hash3 = hashCalculator3.getHash();

        assertThat(hash1) // structural equality
                .isEqualTo(hash2);

        assertThat(hash1) // same reference - different structure
                .isNotEqualTo(hash3);
    }

    private static Map<String, FacetResult> facet(IDocumentSession session, int skip, int take, Reference<QueryStatistics> statsRef) {
        FacetOptions facetOptions = new FacetOptions();
        facetOptions.setStart(skip);
        facetOptions.setPageSize(take);

        Map<String, FacetResult> result = session.query(Result.class, ContactsIndex.class)
                .statistics(statsRef)
                .orderBy("companyId", OrderingType.ALPHA_NUMERIC)
                .whereEquals("active", true)
                .whereEquals("tags", "apple")
                .aggregateBy(b -> b.byField("companyId").withOptions(facetOptions))
                .execute();

        return result;
    }

    public static class Contact {
        private String id;
        private int companyId;
        private boolean active;
        private String[] tags;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getCompanyId() {
            return companyId;
        }

        public void setCompanyId(int companyId) {
            this.companyId = companyId;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String[] getTags() {
            return tags;
        }

        public void setTags(String[] tags) {
            this.tags = tags;
        }
    }


    public static class ContactsIndex extends AbstractIndexCreationTask {
        public ContactsIndex() {
            this.map = "from contact in docs.contacts select new { companyId = contact.companyId, tags = contact.tags, active = contact.active }";
        }
    }

    public static class Result {
        private int companyId;
        private boolean active;
        private String[] tags;

        public int getCompanyId() {
            return companyId;
        }

        public void setCompanyId(int companyId) {
            this.companyId = companyId;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public String[] getTags() {
            return tags;
        }

        public void setTags(String[] tags) {
            this.tags = tags;
        }
    }

}
