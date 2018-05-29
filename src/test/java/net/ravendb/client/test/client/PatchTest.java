package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.patching.JavaScriptException;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.test.client.indexing.IndexesFromClientTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PatchTest extends RemoteTestBase {

    @Test
    public void canPatchSingleDocument() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");

                session.store(user, "users/1");
                session.saveChanges();
            }

            PatchOperation patchOperation = new PatchOperation("users/1", null,
                    PatchRequest.forScript("this.name = \"Patched\""));
            PatchStatus status = store.operations().send(patchOperation);
            assertThat(status)
                    .isEqualTo(PatchStatus.PATCHED);

            try (IDocumentSession session = store.openSession()) {
                User loadedUser = session.load(User.class, "users/1");

                assertThat(loadedUser.getName())
                        .isEqualTo("Patched");
            }

        }
    }

    @Test
    public void canWaitForIndexAfterPatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            new IndexesFromClientTest.Users_ByName().execute(store);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");

                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().waitForIndexesAfterSaveChanges(x -> x.waitForIndexes("Users/ByName"));

                User user = session.load(User.class, "users/1");
                session.advanced().patch(user, "name", "New Name");
                session.saveChanges();
            }
        }
    }

    @Test
    public void canPatchManyDocuments() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");

                session.store(user, "users/1");
                session.saveChanges();

                assertThat(session.query(User.class)
                            .countLazily()
                            .getValue())
                        .isEqualTo(1);
            }



            PatchByQueryOperation operation = new PatchByQueryOperation("from Users update {  this.name= \"Patched\"  }");

            Operation op = store.operations().sendAsync(operation);

            op.waitForCompletion();

            try (IDocumentSession session = store.openSession()) {
                User loadedUser = session.load(User.class, "users/1");

                assertThat(loadedUser.getName())
                        .isEqualTo("Patched");
            }
        }
    }

    @Test
    public void throwsOnInvalidScript() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");

                session.store(user, "users/1");
                session.saveChanges();
            }


            PatchByQueryOperation operation = new PatchByQueryOperation("from Users update {  throw 5 }");

            Operation op = store.operations().sendAsync(operation);

            assertThatThrownBy(() -> op.waitForCompletion())
                    .isExactlyInstanceOf(JavaScriptException.class);
        }
    }

}
