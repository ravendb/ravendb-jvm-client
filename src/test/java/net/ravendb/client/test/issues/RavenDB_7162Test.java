package net.ravendb.client.test.issues;

import net.ravendb.client.DatabaseCommands;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.indexes.StopIndexingOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.infrastructure.entities.Person;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_7162Test extends RemoteTestBase {

    @Test
    public void requestTimeoutShouldWork() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.maintenance().send(new StopIndexingOperation());

            try (IDocumentSession session = store.openSession()) {
                Person person = new Person();
                person.setName("John");
                session.store(person);
                session.saveChanges();
            }

            try (CleanCloseable withTimeout = store.setRequestTimeout(Duration.ofMillis(100))) {
                try (DatabaseCommands commands = DatabaseCommands.forStore(store)) {
                    assertThatThrownBy(() -> commands.execute(new DelayCommand(Duration.ofSeconds(2))))
                            .isInstanceOf(RavenException.class)
                            .hasMessageContaining("failed with timeout after 00:00:00.1000000");
                }
            }
        }
    }

    @Test
    public void requestWithTimeoutShouldWork() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.maintenance().send(new StopIndexingOperation());

            try (IDocumentSession session = store.openSession()) {
                Person person = new Person();
                person.setName("John");
                session.store(person);
                session.saveChanges();
            }

            try (CleanCloseable withTimeout = store.setRequestTimeout(Duration.ofMillis(100))) {
                try (DatabaseCommands commands = DatabaseCommands.forStore(store)) {
                    commands.execute(new DelayCommand(Duration.ofMillis(2)));
                }
            }
        }
    }

    public static class DelayCommand extends VoidRavenCommand {
        private final Duration _value;

        public DelayCommand(Duration value) {
            _value = value;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/test/delay?value=" + _value.toMillis();

            return new HttpGet();
        }
    }
}
