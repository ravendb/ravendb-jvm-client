package net.ravendb.client.test.client.documents;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Person;
import net.ravendb.client.infrastructure.entities.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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
                Assertions.assertThat(user).isNull();
            }

            try (IDocumentSession session = store.openSession()) {
                Person person = new Person();
                person.setName("Grisha");

                session.store(person, documentId);
                session.saveChanges();
            }
        }
    }

    /* TODO


        [Fact]
        public async Task GetAsync()
        {
            using (var store = GetDocumentStore())
            {
                var dummy = JObject.FromObject(new User());
                dummy.Remove("Id");

                using (var session = store.OpenAsyncSession())
                {
                    await session.StoreAsync(new User { Name = "Fitzchak" }, "users/1");
                    await session.StoreAsync(new User { Name = "Arek" }, "users/2");

                    await session.SaveChangesAsync();
                }

                var requestExecuter = store
                    .GetRequestExecutor();

                using (requestExecuter.ContextPool.AllocateOperationContext(out JsonOperationContext context))
                {
                    var getDocumentCommand = new GetDocumentCommand(new[] { "users/1", "users/2" }, includes: null, metadataOnly: false);

                    requestExecuter
                        .Execute(getDocumentCommand, context);

                    var docs = getDocumentCommand.Result;
                    Assert.Equal(2, docs.Results.Length);

                    var doc1 = docs.Results[0] as BlittableJsonReaderObject;
                    var doc2 = docs.Results[1] as BlittableJsonReaderObject;

                    Assert.NotNull(doc1);

                    var doc1Properties = doc1.GetPropertyNames();
                    Assert.True(doc1Properties.Contains("@metadata"));
                    Assert.Equal(dummy.Count + 1, doc1Properties.Length); // +1 for @metadata

                    Assert.NotNull(doc2);

                    var doc2Properties = doc2.GetPropertyNames();
                    Assert.True(doc2Properties.Contains("@metadata"));
                    Assert.Equal(dummy.Count + 1, doc2Properties.Length); // +1 for @metadata

                    using (var session = (DocumentSession)store.OpenSession())
                    {
                        var user1 = (User)session.EntityToBlittable.ConvertToEntity(typeof(User), "users/1", doc1);
                        var user2 = (User)session.EntityToBlittable.ConvertToEntity(typeof(User), "users/2", doc2);

                        Assert.Equal("Fitzchak", user1.Name);
                        Assert.Equal("Arek", user2.Name);
                    }

                    getDocumentCommand = new GetDocumentCommand(new[] {"users/1", "users/2"}, includes: null,
                        metadataOnly: true);

                    requestExecuter
                        .Execute(getDocumentCommand, context);

                    docs = getDocumentCommand.Result;
                    Assert.Equal(2, docs.Results.Length);

                    doc1 = docs.Results[0] as BlittableJsonReaderObject;
                    doc2 = docs.Results[1] as BlittableJsonReaderObject;

                    Assert.NotNull(doc1);

                    doc1Properties = doc1.GetPropertyNames();
                    Assert.True(doc1Properties.Contains("@metadata"));
                    Assert.Equal(1, doc1Properties.Length);

                    Assert.NotNull(doc2);

                    doc2Properties = doc2.GetPropertyNames();
                    Assert.True(doc2Properties.Contains("@metadata"));
                    Assert.Equal(1, doc2Properties.Length);
                }
            }
        }
     */
}
