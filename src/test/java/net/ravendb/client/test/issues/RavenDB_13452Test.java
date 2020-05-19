package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.setAllowComparingPrivateFields;

public class RavenDB_13452Test extends RemoteTestBase {

    public static class Item {
        private Map<String, String> values;

        public Map<String, String> getValues() {
            return values;
        }

        public void setValues(Map<String, String> values) {
            this.values = values;
        }
    }

    @Test
    public void canModifyDictionaryWithPatch_Add() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Item item = new Item();
                Map<String, String> values = new HashMap<>();
                item.setValues(values);
                values.put("Key1", "Value1");
                values.put("Key2", "Value2");

                session.store(item, "items/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Item item = session.load(Item.class, "items/1");
                session.advanced().patchObject(item, "values", dict -> dict.put("Key3", "Value3"));
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ObjectNode item = session.load(ObjectNode.class, "items/1");
                JsonNode values = item.get("values");
                assertThat(values)
                        .isNotNull()
                        .hasSize(3);

                assertThat(values.get("Key1")
                        .asText())
                        .isEqualTo("Value1");

                assertThat(values.get("Key2")
                        .asText())
                        .isEqualTo("Value2");

                assertThat(values.get("Key3")
                        .asText())
                        .isEqualTo("Value3");
            }
        }
    }

    @Test
    public void canModifyDictionaryWithPatch_Remove() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Item item = new Item();
                Map<String, String> values = new HashMap<>();
                item.setValues(values);
                values.put("Key1", "Value1");
                values.put("Key2", "Value2");
                values.put("Key3", "Value3");

                session.store(item, "items/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Item item = session.load(Item.class, "items/1");
                session.advanced().patchObject(item, "values",  dict -> dict.remove("Key2"));
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ObjectNode item = session.load(ObjectNode.class, "items/1");
                JsonNode values = item.get("values");
                assertThat(values)
                        .isNotNull()
                        .hasSize(2);

                assertThat(values.get("Key1")
                        .asText())
                        .isEqualTo("Value1");

                assertThat(values.get("Key3")
                        .asText())
                        .isEqualTo("Value3");
            }

        }
    }


    /* TODO




        [Fact]
        public void CanModifyDictionaryWithPatch_Remove()
        {
            using (var store = GetDocumentStore())
            {

                using (var session = store.OpenSession())
                {
                    var item = session.Load<Item>("items/1");
                    session.Advanced.Patch(item, x => x.Values, dict => dict.Remove("Key2"));
                    session.SaveChanges();
                }
                using (var commands = store.Commands())
                {
                    var item = commands.Get("items/1").BlittableJson;
                    Assert.True(item.TryGet(nameof(Item.Values), out BlittableJsonReaderObject values));
                    Assert.Equal(2, values.Count);

                    Assert.True(values.TryGet("Key1", out string value));
                    Assert.Equal("Value1", value);
                    Assert.True(values.TryGet("Key3", out value));
                    Assert.Equal("Value3", value);
                }
            }
        }





     */
}
