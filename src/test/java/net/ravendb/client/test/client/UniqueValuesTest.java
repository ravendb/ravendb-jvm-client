package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.*;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UniqueValuesTest extends RemoteTestBase {

    @Test
    public void canPutUniqueString() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (DocumentSession session = (DocumentSession) store.openSession()) {
                store.operations().send(new PutCompareExchangeValueOperation<>("test", "Karmel", 0));
                CmpXchgResult<String> res = store.operations().send(new GetCompareExchangeValueOperation<>(String.class, "test"));
                assertThat(res.getValue())
                        .isEqualTo("Karmel");
                assertThat(res.isSuccessful())
                        .isTrue();
            }
        }
    }

    @Test
    public void canPutMultiDifferentValues() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user1 = new User();
            user1.setName("Karmel");

            CmpXchgResult<User> res = store.operations().send(new PutCompareExchangeValueOperation<>("test", user1, 0));

            User user2 = new User();
            user2.setName("Karmel");

            CmpXchgResult<User> res2 = store.operations().send(new PutCompareExchangeValueOperation<>("test2", user2, 0));


            assertThat(res.getValue().getName())
                    .isEqualTo("Karmel");
            assertThat(res.isSuccessful())
                    .isTrue();

            assertThat(res2.getValue().getName())
                    .isEqualTo("Karmel");
            assertThat(res2.isSuccessful())
                    .isTrue();
        }
    }

    @Test
    public void canListCompareExchange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user1 = new User();
            user1.setName("Karmel");
            store.operations().send(new PutCompareExchangeValueOperation<>("test", user1, 0));

            User user2 = new User();
            user2.setName("Karmel");
            store.operations().send(new PutCompareExchangeValueOperation<>("test2", user2, 0));

            List<ListCompareExchangeValuesOperation.CompareExchangeItem> list =
                    store.operations().send(new ListCompareExchangeValuesOperation("test"));

            assertThat(list)
                    .hasSize(2);

            assertThat(list.get(0).getKey())
                    .isEqualTo("test");

            assertThat(JsonExtensions.getDefaultEntityMapper().convertValue(list.get(0).getValue(), User.class).getName())
                    .isEqualTo("Karmel");

            assertThat(list.get(1).getKey())
                    .isEqualTo("test2");

            assertThat(JsonExtensions.getDefaultEntityMapper().convertValue(list.get(1).getValue(), User.class).getName())
                    .isEqualTo("Karmel");
        }
    }

    @Test
    public void canRemoveUnique() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            CmpXchgResult<String> res = store.operations().send(new PutCompareExchangeValueOperation<>("test", "Karmel", 0));

            assertThat(res.getValue())
                    .isEqualTo("Karmel");

            assertThat(res.isSuccessful())
                    .isTrue();

            res = store.operations().send(new RemoveCompareExchangeOperation<>(String.class,"test", res.getIndex()));
            assertThat(res.isSuccessful())
                    .isTrue();

        }
    }

    @Test
    public void removeUniqueFailed() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            CmpXchgResult<String> res = store.operations().send(new PutCompareExchangeValueOperation<>("test", "Karmel", 0));
            assertThat(res.getValue())
                    .isEqualTo("Karmel");

            assertThat(res.isSuccessful())
                    .isTrue();

            res = store.operations().send(new RemoveCompareExchangeOperation<>(String.class, "test", 0));
            assertThat(res.isSuccessful())
                    .isFalse();

            res = store.operations().send(new GetCompareExchangeValueOperation<>(String.class, "test"));
            assertThat(res.getValue())
                    .isEqualTo("Karmel");

            assertThat(res.isSuccessful())
                    .isTrue();
        }
    }

    @Test
    public void returnCurrentValueWhenPuttingConcurrently() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user = new User();
            user.setName("Karmel");

            User user2 = new User();
            user2.setName("Karmel2");

            CmpXchgResult<User> res = store.operations().send(new PutCompareExchangeValueOperation<>("test", user, 0));
            CmpXchgResult<User> res2 = store.operations().send(new PutCompareExchangeValueOperation<>("test", user2, 0));

            assertThat(res.isSuccessful())
                    .isTrue();
            assertThat(res2.isSuccessful())
                    .isFalse();
            assertThat(res.getValue().getName())
                    .isEqualTo("Karmel");
            assertThat(res2.getValue().getName())
                    .isEqualTo("Karmel");

            User user3 = new User();
            user3.setName("Karmel2");

            res2 = store.operations().send(new PutCompareExchangeValueOperation<>("test", user3, res2.getIndex()));
            assertThat(res2.isSuccessful())
                    .isTrue();

            assertThat(res2.getValue().getName())
                    .isEqualTo("Karmel2");
        }
    }

    @Test
    public void canGetIndexValue() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user = new User();
            user.setName("Karmel");

            store.operations().send(new PutCompareExchangeValueOperation<>("test", user, 0));
            CmpXchgResult<User> res = store.operations().send(new GetCompareExchangeValueOperation<>(User.class, "test"));

            assertThat(res.getValue().getName())
                    .isEqualTo("Karmel");
            assertThat(res.isSuccessful())
                    .isTrue();

            User user2 = new User();
            user2.setName("Karmel2");

            CmpXchgResult<User> res2 = store.operations().send(new PutCompareExchangeValueOperation<>("test", user2, res.getIndex()));
            assertThat(res2.isSuccessful())
                    .isTrue();

            assertThat(res2.getValue().getName())
                    .isEqualTo("Karmel2");
        }
    }
}
