package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.NextIdentityForCommand;
import net.ravendb.client.documents.commands.SeedIdentityForCommand;
import net.ravendb.client.documents.operations.GetIdentitiesOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class NextAndSeedIdentitiesTest extends RemoteTestBase {

    @Test
    public void nextIdentityFor() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setLastName("Adi");

                session.store(user, "users|");
                session.saveChanges();
            }

            NextIdentityForCommand command = new NextIdentityForCommand("users");
            store.getRequestExecutor().execute(command);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setLastName("Avivi");

                session.store(user, "users|");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User entityWithId1 = session.load(User.class, "users/1");
                User entityWithId2 = session.load(User.class, "users/2");
                User entityWithId3 = session.load(User.class, "users/3");
                User entityWithId4 = session.load(User.class, "users/4");

                assertThat(entityWithId1)
                        .isNotNull();
                assertThat(entityWithId3)
                        .isNotNull();
                assertThat(entityWithId2)
                        .isNull();
                assertThat(entityWithId4)
                        .isNull();

                assertThat(entityWithId1.getLastName())
                        .isEqualTo("Adi");
                assertThat(entityWithId3.getLastName())
                        .isEqualTo("Avivi");
            }
        }
    }

    @Test
    public void seedIdentityFor() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setLastName("Adi");

                session.store(user, "users|");
                session.saveChanges();
            }

            SeedIdentityForCommand command = new SeedIdentityForCommand("users", 1990L);
            store.getRequestExecutor().execute(command);
            Long result = command.getResult();
            assertThat(result)
                    .isEqualTo(1990L);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setLastName("Avivi");
                session.store(user, "users|");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User entityWithId1 = session.load(User.class, "users/1");
                User entityWithId2 = session.load(User.class, "users/2");
                User entityWithId1990 = session.load(User.class, "users/1990");
                User entityWithId1991 = session.load(User.class, "users/1991");
                User entityWithId1992 = session.load(User.class, "users/1992");

                assertThat(entityWithId1)
                        .isNotNull();
                assertThat(entityWithId1991)
                        .isNotNull();
                assertThat(entityWithId2)
                        .isNull();
                assertThat(entityWithId1990)
                        .isNull();
                assertThat(entityWithId1992)
                        .isNull();

                assertThat(entityWithId1.getLastName())
                        .isEqualTo("Adi");
                assertThat(entityWithId1991.getLastName())
                        .isEqualTo("Avivi");
            }

            command = new SeedIdentityForCommand("users", 1975L);
            store.getRequestExecutor().execute(command);
            assertThat(command.getResult())
                    .isEqualTo(1991L);


            Map<String, Long> identities = store.maintenance().send(new GetIdentitiesOperation());

            assertThat(identities.get("users|"))
                    .isEqualTo(1991);
        }
    }
}
