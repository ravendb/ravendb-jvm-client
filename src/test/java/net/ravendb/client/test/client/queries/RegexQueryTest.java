package net.ravendb.client.test.client.queries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RegexQueryTest extends RemoteTestBase {

    @Test
    public void queriesWithRegexFromDocumentQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new RegexMe("I love dogs and cats"));
                session.store(new RegexMe("I love cats"));
                session.store(new RegexMe("I love dogs"));
                session.store(new RegexMe("I love bats"));
                session.store(new RegexMe("dogs love me"));
                session.store(new RegexMe("cats love me"));
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<RegexMe> query = session.
                        advanced()
                        .documentQuery(RegexMe.class)
                        .whereRegex("text", "^[a-z ]{2,4}love");

                IndexQuery iq = query.getIndexQuery();

                assertThat(iq.getQuery())
                        .isEqualTo("from 'RegexMes' where regex(text, $p0)");

                assertThat(iq.getQueryParameters().get("p0"))
                        .isEqualTo("^[a-z ]{2,4}love");

                List<RegexMe> result = query.toList();
                assertThat(result)
                        .hasSize(4);
            }
        }
    }

    public static class RegexMe {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public RegexMe() {
        }

        public RegexMe(String text) {
            this.text = text;
        }
    }
}
