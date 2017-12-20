package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.batches.PutResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PutDocumentCommandTest extends RemoteTestBase {

    @Test
    public void canPutDocumentUsingCommand() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            User user = new User();
            user.setName("Marcin");
            user.setAge(30);

            ObjectNode node = JsonExtensions.getDefaultEntityMapper().valueToTree(user);

            PutDocumentCommand command = new PutDocumentCommand("users/1", null, node);
            store.getRequestExecutor().execute(command);

            PutResult result = command.getResult();

            assertThat(result.getId())
                    .isEqualTo("users/1");

            assertThat(result.getChangeVector())
                    .isNotNull();

            try (IDocumentSession session = store.openSession()) {
                User loadedUser = session.load(User.class, "users/1");

                assertThat(loadedUser.getName())
                        .isEqualTo("Marcin");
            }
        }
    }
}

