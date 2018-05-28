package net.ravendb.client.test.server.replication;

import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetConflictsCommand;
import net.ravendb.client.documents.commands.GetConflictsResult;
import net.ravendb.client.documents.commands.PutDocumentCommand;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.DocumentConflictException;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.ConflictSolver;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DocumentReplicationTest extends ReplicationTestBase {

    private Consumer<DatabaseRecord> customize = null;

    @Override
    protected void customizeDbRecord(DatabaseRecord dbRecord) {
        if (customize != null) {
            customize.accept(dbRecord);
        }
    }

    @Test
    public void canReplicateDocument() throws Exception {
        customize = r -> {
            ConflictSolver conflictSolver = new ConflictSolver();
            conflictSolver.setResolveToLatest(false);
            conflictSolver.setResolveByCollection(Collections.emptyMap());
            r.setConflictSolverConfig(conflictSolver);
        };

        try (IDocumentStore source = getDocumentStore()) {
            try (IDocumentStore destination = getDocumentStore()) {

                String id;
                setupReplication(source, destination);

                try (IDocumentSession session = source.openSession()) {
                    User user = new User();
                    user.setName("Arek");
                    session.store(user);
                    session.saveChanges();

                    id = user.getId();
                }

                User fetchedUser = waitForDocumentToReplicate(destination, User.class, id, 10_000);
                assertThat(fetchedUser)
                        .isNotNull();

                assertThat(fetchedUser.getName())
                        .isEqualTo("Arek");
            }
        } finally {
            customize = null;
        }
    }

    @Test
    public void getConflictsResult_command_should_work_properly() throws Exception {
        customize = r -> {
            ConflictSolver conflictSolver = new ConflictSolver();
            conflictSolver.setResolveToLatest(false);
            conflictSolver.setResolveByCollection(Collections.emptyMap());
            r.setConflictSolverConfig(conflictSolver);
        };

        try (IDocumentStore source = getDocumentStore()) {
            try (IDocumentStore destination = getDocumentStore()) {

                try (IDocumentSession session = source.openSession()) {
                    User user1 = new User();
                    user1.setName("Value");

                    session.store(user1, "docs/1");
                    session.saveChanges();
                }

                try (IDocumentSession session = destination.openSession()) {
                    User user1 = new User();
                    user1.setName("Value2");

                    session.store(user1, "docs/1");
                    session.saveChanges();
                }

                setupReplication(source, destination);

                try (IDocumentSession session = source.openSession()) {
                    User user1 = new User();
                    user1.setName("marker");

                    session.store(user1, "marker");
                    session.saveChanges();
                }

                waitForDocumentToReplicate(destination, User.class, "marker", 2_000);

                GetConflictsCommand command = new GetConflictsCommand("docs/1");
                destination.getRequestExecutor().execute(command);
                GetConflictsResult conflicts = command.getResult();

                assertThat(conflicts.getResults())
                        .hasSize(2);

                assertThat(conflicts.getResults()[0].getChangeVector())
                        .isNotEqualTo(conflicts.getResults()[1].getChangeVector());
            }
        } finally {
            customize = null;
        }
    }

    @Test
    public void shouldCreateConflictThenResolveIt() throws Exception {
        customize = r -> {
            ConflictSolver conflictSolver = new ConflictSolver();
            conflictSolver.setResolveToLatest(false);
            conflictSolver.setResolveByCollection(Collections.emptyMap());
            r.setConflictSolverConfig(conflictSolver);
        };

        try (IDocumentStore source = getDocumentStore()) {
            try (IDocumentStore destination = getDocumentStore()) {

                try (IDocumentSession session = source.openSession()) {
                    User user1 = new User();
                    user1.setName("Value");

                    session.store(user1, "docs/1");
                    session.saveChanges();
                }

                try (IDocumentSession session = destination.openSession()) {
                    User user1 = new User();
                    user1.setName("Value2");

                    session.store(user1, "docs/1");
                    session.saveChanges();
                }

                setupReplication(source, destination);

                try (IDocumentSession session = source.openSession()) {
                    User user1 = new User();
                    user1.setName("marker");

                    session.store(user1, "marker");
                    session.saveChanges();
                }

                waitForDocumentToReplicate(destination, User.class, "marker", 2_000);

                GetConflictsCommand command = new GetConflictsCommand("docs/1");
                destination.getRequestExecutor().execute(command);
                GetConflictsResult conflicts = command.getResult();

                assertThat(conflicts.getResults())
                        .hasSize(2);

                assertThat(conflicts.getResults()[0].getChangeVector())
                        .isNotEqualTo(conflicts.getResults()[1].getChangeVector());

                assertThatThrownBy(() -> {
                    try (IDocumentSession session = destination.openSession()) {
                        session.load(User.class, "docs/1");
                    }
                }).isExactlyInstanceOf(DocumentConflictException.class);

                //now actually resolve the conflict
                //(resolve by using first variant)
                PutDocumentCommand putCommand = new PutDocumentCommand("docs/1", null, conflicts.getResults()[0].getDoc());
                destination.getRequestExecutor().execute(putCommand);

                try (IDocumentSession session = destination.openSession()) {
                    User loadedUser = session.load(User.class, "docs/1");

                    assertThat(loadedUser.getName())
                            .isEqualTo(conflicts.getResults()[0].getDoc().get("name").asText());
                }
            }
        } finally {
            customize = null;
        }
    }
}
