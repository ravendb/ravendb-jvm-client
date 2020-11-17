package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15693Test extends RemoteTestBase {

    @Test
    public void canQueryOnComplexBoost() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession s = store.openSession()) {
                IDocumentQuery<Doc> q = s.advanced().documentQuery(Doc.class)
                        .search("strVal1", "a")
                        .andAlso()
                        .openSubclause()
                        .search("strVal2", "b")
                        .orElse()
                        .search("strVal3", "search")
                        .closeSubclause()
                        .boost(0.2);

                String queryBoost = q.toString();

                assertThat(queryBoost)
                        .isEqualTo("from 'Docs' where search(strVal1, $p0) and boost(search(strVal2, $p1) or search(strVal3, $p2), $p3)");

                q.toList();
            }
        }
    }

    public static class Doc {
        private String strVal1;
        private String strVal2;
        private String strVal3;

        public String getStrVal1() {
            return strVal1;
        }

        public void setStrVal1(String strVal1) {
            this.strVal1 = strVal1;
        }

        public String getStrVal2() {
            return strVal2;
        }

        public void setStrVal2(String strVal2) {
            this.strVal2 = strVal2;
        }

        public String getStrVal3() {
            return strVal3;
        }

        public void setStrVal3(String strVal3) {
            this.strVal3 = strVal3;
        }
    }
}

