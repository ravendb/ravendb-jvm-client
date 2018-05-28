package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;
import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.QueryCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.ConflictException;
import net.ravendb.client.exceptions.documents.DocumentConflictException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.entities.Address;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.ConflictSolver;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("SameParameterValue")
public class RavenDB_6292Test extends ReplicationTestBase {

    private Consumer<DatabaseRecord> customize = null;

    @Override
    protected void customizeDbRecord(DatabaseRecord dbRecord) {
        if (customize != null) {
            customize.accept(dbRecord);
        }
    }

    @Test
    public void ifIncludedDocumentIsConflictedItShouldNotThrowConflictException() throws Exception {
        customize = r -> {
            ConflictSolver conflictSolver = new ConflictSolver();
            conflictSolver.setResolveToLatest(false);
            conflictSolver.setResolveByCollection(Collections.emptyMap());
            r.setConflictSolverConfig(conflictSolver);
        };

        try (IDocumentStore store1 = getDocumentStore()) {
            try (IDocumentStore store2 = getDocumentStore()) {

                try (IDocumentSession session = store1.openSession()) {
                    Address address = new Address();
                    address.setCity("New York");
                    session.store(address, "addresses/1");
                    session.saveChanges();
                }

                try (IDocumentSession session = store2.openSession()) {
                    Address address = new Address();
                    address.setCity("Torun");
                    session.store(address, "addresses/1");

                    User user = new User();
                    user.setName("John");
                    user.setAddressId("addresses/1");
                    session.store(user, "users/1");

                    session.saveChanges();
                }

                setupReplication(store1, store2);

                waitForConflict(store2, "addresses/1");

                try (IDocumentSession session = store2.openSession()) {
                    IDocumentQuery<User> documentQuery = session.advanced()
                            .documentQuery(User.class)
                            .include("addressId");

                    IndexQuery iq = documentQuery.getIndexQuery();

                    User user = documentQuery.first();

                    assertThat(user.getName())
                            .isEqualTo("John");

                    assertThatThrownBy(() -> session.load(Address.class, user.getAddressId())).isExactlyInstanceOf(DocumentConflictException.class);

                    QueryCommand queryCommand = new QueryCommand(DocumentConventions.defaultConventions, iq, false, false);

                    store2.getRequestExecutor().execute(queryCommand);

                    QueryResult result = queryCommand.getResult();
                    JsonNode address = result.getIncludes().get("addresses/1");
                    JsonNode metadata = address.get("@metadata");
                    assertThat(metadata.get("@id").asText())
                            .isEqualTo("addresses/1");

                    assertThat(JsonExtensions.tryGetConflict(metadata))
                            .isTrue();
                }
            }
        }
    }

    private static void waitForConflict(IDocumentStore store, String id) throws InterruptedException {
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.MILLISECONDS) < 10_000) {
            try (IDocumentSession session = store.openSession()) {
                session.load(Object.class, id);

                Thread.sleep(10);
            } catch (ConflictException e) {
                return;
            }
        }

        throw new IllegalStateException("Waited for conflict on '" + id + "' but it did not happen");
    }
}
