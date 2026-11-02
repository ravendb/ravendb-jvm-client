package net.ravendb.client.documents.queries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.AbstractDocumentQuery;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IncludesAliasTokensTest extends RemoteTestBase {

    @Test
    public void addAliasToIncludesTokensAliasesWhereTokensAndCounterIncludes() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<User> query = session.query(User.class)
                        .include(i -> i.includeCounter("addressId", "likes"))
                        .whereEquals("name", "Jerry");

                assertThat(query.toString())
                        .isEqualTo("from 'Users' where name = $p0 include counters(addressId, 'likes')");

                // what the C# LINQ provider does when the query has a counters include with an alias
                String alias = ((AbstractDocumentQuery<?, ?>) query).addAliasToIncludesTokens(null);

                assertThat(alias).isNotEmpty();
                assertThat(query.toString())
                        .isEqualTo("from 'Users' where " + alias + ".name = $p0 include counters(" + alias + ".addressId, 'likes')");
            }
        }
    }

    @Test
    public void addAliasToIncludesTokensKeepsExplicitFromAlias() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<User> query = session.query(User.class)
                        .include(i -> i.includeCounter("addressId", "likes"))
                        .whereEquals("name", "Jerry");

                // with an existing from-alias only the include paths are aliased; where tokens are left to the caller
                String alias = ((AbstractDocumentQuery<?, ?>) query).addAliasToIncludesTokens("u");

                assertThat(alias).isEqualTo("u");
                assertThat(query.toString())
                        .isEqualTo("from 'Users' where name = $p0 include counters(u.addressId, 'likes')");
            }
        }
    }
}
