package net.ravendb.client.test.server.replication;

import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetConflictsCommand;
import net.ravendb.client.documents.commands.GetConflictsResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.operations.ModifyConflictSolverOperation;
import net.ravendb.client.serverwide.operations.ModifySolverResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ConflictSolverTest extends ReplicationTestBase {

    @Test
    @DisabledOnPullRequest
    public void getConflictsResult_command_should_work_properly() throws Exception {
        try (IDocumentStore source = getDocumentStore()) {
            try (IDocumentStore destination = getDocumentStore()) {

                ModifyConflictSolverOperation conflictSolverOperation = new ModifyConflictSolverOperation(destination.getDatabase(), Collections.emptyMap(), false);
                ModifySolverResult solverResult =
                        destination.maintenance().server().send(conflictSolverOperation);

                assertThat(solverResult)
                        .isNotNull();
                assertThat(solverResult.getKey())
                        .isEqualTo(destination.getDatabase());
                assertThat(solverResult.getRaftCommandIndex())
                        .isPositive();

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
            }
        }
    }
}
