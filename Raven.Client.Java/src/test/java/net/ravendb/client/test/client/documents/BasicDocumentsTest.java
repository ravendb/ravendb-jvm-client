package net.ravendb.client.test.client.documents;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetDocumentsCommand;
import net.ravendb.client.documents.commands.GetDocumentsResult;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.entities.Person;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicDocumentsTest extends RemoteTestBase {

    @Test
    public void canChangeDocumentCollectionWithDeleteAndSave() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            String documentId = "users/1";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Grisha");

                session.store(user, documentId);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.delete(documentId);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, documentId);
                assertThat(user).isNull();
            }

            try (IDocumentSession session = store.openSession()) {
                Person person = new Person();
                person.setName("Grisha");

                session.store(person, documentId);
                session.saveChanges();
            }
        }
    }

    @Test
    public void get() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            ObjectNode dummy = JsonExtensions.getDefaultMapper().valueToTree(new User());
            dummy.remove("id");

            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Fitzchak");

                User user2 = new User();
                user2.setName("Arek");

                session.store(user1, "users/1");
                session.store(user2, "users/2");

                session.saveChanges();
            }

            RequestExecutor requestExecutor = store.getRequestExecutor();

            GetDocumentsCommand getDocumentsCommand = new GetDocumentsCommand(new String[]{"users/1", "users/2"}, null, false);

            requestExecutor.execute(getDocumentsCommand);

            GetDocumentsResult docs = getDocumentsCommand.getResult();
            assertThat(docs.getResults().size())
                    .isEqualTo(2);

            ObjectNode doc1 = (ObjectNode) docs.getResults().get(0);
            ObjectNode doc2 = (ObjectNode) docs.getResults().get(1);

            assertThat(doc1)
                    .isNotNull();

            ArrayList<String> doc1Properties = Lists.newArrayList(doc1.fieldNames());
            assertThat(doc1Properties)
                    .contains("@metadata");

            assertThat(doc1Properties.size())
                    .isEqualTo(dummy.size() + 1); // +1 for @metadata

            assertThat(doc2)
                    .isNotNull();

            ArrayList<String> doc2Properties = Lists.newArrayList(doc2.fieldNames());
            assertThat(doc2Properties)
                    .contains("@metadata");

            assertThat(doc2Properties.size())
                    .isEqualTo(dummy.size() + 1); // +1 for @metadata

            try (DocumentSession session = (DocumentSession) store.openSession()) {
                User user1 = (User) session.getEntityToJson().convertToEntity(User.class, "users/1", doc1);
                User user2 = (User) session.getEntityToJson().convertToEntity(User.class, "users/2", doc2);

                assertThat(user1.getName())
                        .isEqualTo("Fitzchak");

                assertThat(user2.getName())
                        .isEqualTo("Arek");
            }

            getDocumentsCommand = new GetDocumentsCommand(new String[] { "users/1", "users/2"}, null, true);

            requestExecutor.execute(getDocumentsCommand);

            docs = getDocumentsCommand.getResult();

            assertThat(docs.getResults())
                    .hasSize(2);

            doc1 = (ObjectNode) docs.getResults().get(0);
            doc2 = (ObjectNode) docs.getResults().get(1);

            assertThat(doc1)
                    .isNotNull();

            doc1Properties = Lists.newArrayList(doc1.fieldNames());
            assertThat(doc1Properties)
                    .contains("@metadata");

            assertThat(doc1Properties.size())
                    .isEqualTo(1);

            assertThat(doc2)
                    .isNotNull();

            doc2Properties = Lists.newArrayList(doc2.fieldNames());
            assertThat(doc2Properties)
                    .contains("@metadata");

            assertThat(doc2Properties.size())
                    .isEqualTo(1);

        }
    }
}
