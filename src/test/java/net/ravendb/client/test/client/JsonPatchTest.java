package net.ravendb.client.test.client;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.PutDocumentCommand;
import net.ravendb.client.documents.commands.batches.JsonPatchCommandData;
import net.ravendb.client.documents.commands.batches.JsonPatchDocument;
import net.ravendb.client.documents.operations.JsonPatchOperation;
import net.ravendb.client.documents.operations.JsonPatchResult;
import net.ravendb.client.documents.operations.PatchStatus;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.extensions.JsonExtensions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonPatchTest extends RemoteTestBase {

    private static final String DOC_ID = "docs/1";

    private static final ObjectMapper MAPPER = JsonExtensions.getDefaultMapper();

    @Test
    public void patchingWithEscaping() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'~':'~','/':'/','foo/bar~':'foo/bar~'," +
                    "'biscuits':[{'~':'Nested~'},{'/':'Nested/'},{'foo/bar~':'NestedFoo/Bar~'}]}");

            JsonPatchDocument jpd = new JsonPatchDocument()
                    .add("/~0", "Hibernating Rhinos1")
                    .replace("/~1", "Hibernating Rhinos2")
                    .add("/foo~1bar~0", "Hibernating Rhinos3")
                    .add("/biscuits/0/~0", "Hibernating Rhinos1")
                    .add("/biscuits/1/~1", "Hibernating Rhinos2")
                    .replace("/biscuits/1/~1", "Hibernating Rhinos replaced")
                    .add("/biscuits/2/foo~1bar~0", "Hibernating Rhinos3");

            JsonPatchResult result = store.operations().send(new JsonPatchOperation(DOC_ID, jpd));
            assertThat(result.getStatus()).isEqualTo(PatchStatus.PATCHED);

            assertDocument(store, "{'~':'Hibernating Rhinos1','/':'Hibernating Rhinos2','foo/bar~':'Hibernating Rhinos3'," +
                    "'biscuits':[{'~':'Hibernating Rhinos1'},{'/':'Hibernating Rhinos replaced'},{'foo/bar~':'Hibernating Rhinos3'}]}");
        }
    }

    @Test
    public void patchingWithAdd() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'The Wall'}");

            send(store, new JsonPatchDocument().add("/Name", "Hibernating Rhinos"));

            assertDocument(store, "{'Name':'Hibernating Rhinos'}");
        }
    }

    @Test
    public void patchingWithAddNonExistentDocId() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'The Wall'}");

            JsonPatchDocument jpd = new JsonPatchDocument().add("/Name", "Hibernating Rhinos");

            assertThatThrownBy(() -> store.operations().send(new JsonPatchOperation(DOC_ID + "1", jpd)))
                    .isInstanceOf(RavenException.class)
                    .hasMessageContaining("Cannot apply json patch because the document " + DOC_ID + "1 does not exist");
        }
    }

    @Test
    public void patchingWithReplaceNestedPathEscaping() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'/':{'Name':'Hibernating'}}");

            send(store, new JsonPatchDocument().replace("/~1/Name", "Developer"));

            assertDocument(store, "{'/':{'Name':'Developer'}}");
        }
    }

    @Test
    public void patchingWithAddNestedPath() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Contact':{'Name':'Hibernating'}}");

            send(store, new JsonPatchDocument().add("/Contact/Title", "Developer"));

            assertDocument(store, "{'Contact':{'Name':'Hibernating','Title':'Developer'}}");
        }
    }

    @Test
    public void patchingWithAddNestedPathDoesntExist() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Contact':{'City':'Hadera','LastName':'Rhinos'}}");

            send(store, new JsonPatchDocument().add("/Contact/FirstName", "Hibernating"));

            assertDocument(store, "{'Contact':{'City':'Hadera','LastName':'Rhinos','FirstName':'Hibernating'}}");
        }
    }

    @Test
    public void patchingWithAddMoreThanOneNonexistentProperty() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Contact':{'Name':'Rhinos'}}");

            JsonPatchDocument jpd = new JsonPatchDocument().add("/Contact/FirstName/ExtraProp", "Hiber");

            assertThatThrownBy(() -> send(store, jpd))
                    .isInstanceOf(RavenException.class)
                    .hasMessageContaining("System.ArgumentException: Cannot reach target location. Failed to fetch 'FirstName' in path '/Contact/FirstName/ExtraProp' for operation 'Add'");
        }
    }

    @Test
    public void patchingWithAddToArrayAtTheEnd() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'MyArray':[1,2,4]}");

            send(store, new JsonPatchDocument().add("/MyArray/-", 5));

            assertDocument(store, "{'MyArray':[1,2,4,5]}");
        }
    }

    @Test
    public void patchingWithAddToArrayAtIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'MyArray':[1,2,4]}");

            send(store, new JsonPatchDocument().add("/MyArray/2", 3));

            assertDocument(store, "{'MyArray':[1,2,3,4]}");
        }
    }

    @Test
    public void patchingWithAddToNestedArraysAtIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'MyArray':[1,2,[22,23]]}");

            send(store, new JsonPatchDocument().add("/MyArray/2/1", 100));

            assertDocument(store, "{'MyArray':[1,2,[22,100,23]]}");
        }
    }

    @Test
    public void patchingWithRemove() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'The Wall','City':'Hadera'}");

            send(store, new JsonPatchDocument().remove("/Name"));

            assertDocument(store, "{'City':'Hadera'}");
        }
    }

    @Test
    public void patchingWithRemoveArrayElement() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'MyArray':[1,2,3]}");

            send(store, new JsonPatchDocument().remove("/MyArray/1"));

            assertDocument(store, "{'MyArray':[1,3]}");
        }
    }

    @Test
    public void patchingWithReplace() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'The Wall'}");

            send(store, new JsonPatchDocument().replace("/Name", "Hibernating Rhinos"));

            assertDocument(store, "{'Name':'Hibernating Rhinos'}");
        }
    }

    @Test
    public void patchingWithReplaceAtNonExistent() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'The Wall','Address':{'City':'Netanya'}}");

            JsonPatchDocument jpd = new JsonPatchDocument().replace("/Address/NonexistentProperty", "Hibernating Rhinos");

            assertThatThrownBy(() -> send(store, jpd))
                    .isInstanceOf(RavenException.class)
                    .hasMessageContaining("System.ArgumentException: Cannot reach target location. Failed to fetch 'NonexistentProperty' in path '/Address/NonexistentProperty' for operation 'Replace'");
        }
    }

    @Test
    public void patchingWithReplaceArrayElement() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':null,'MyArray':[1,2,4]}");

            send(store, new JsonPatchDocument().replace("/MyArray/1", 100));

            assertDocument(store, "{'Name':null,'MyArray':[1,100,4]}");
        }
    }

    @Test
    public void patchingWithMove() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'Hibernating','MyArray':null}");

            send(store, new JsonPatchDocument().move("/Name", "/NewPropName"));

            ObjectNode document = loadDocument(store);
            assertThat(document.has("Name")).isFalse();
            assertThat(document.get("NewPropName").asText()).isEqualTo("Hibernating");
            assertDocument(store, "{'MyArray':null,'NewPropName':'Hibernating'}");
        }
    }

    @Test
    public void patchingWithMoveToNestedInSameObject() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'Hibernating','MyArray':null}");

            JsonPatchDocument jpd = new JsonPatchDocument().move("/Name", "/Name/CompanyName");

            assertThatThrownBy(() -> send(store, jpd)).isInstanceOf(RavenException.class);

            assertDocument(store, "{'Name':'Hibernating','MyArray':null}");
        }
    }

    @Test
    public void patchingWithMoveArrayElementFromIndexToIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'MyArray':[20,40,10,30]}");

            // [20,40,10,30] -> [40,10,20,30] -> [10,20,30,40]
            send(store, new JsonPatchDocument()
                    .move("/MyArray/0", "/MyArray/2")
                    .move("/MyArray/0", "/MyArray/3"));

            assertDocument(store, "{'MyArray':[10,20,30,40]}");
        }
    }

    @Test
    public void patchingWithMultipleAddReplace() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'Hibernating'}");

            send(store, new JsonPatchDocument()
                    .add("/Name", Collections.singletonMap("City", "Netanya"))
                    .replace("/Name/City", "Hadera"));

            assertDocument(store, "{'Name':{'City':'Hadera'}}");
        }
    }

    @Test
    public void patchingWithMultipleAddRemoveObjects() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':null,'MyArray':null}");

            send(store, new JsonPatchDocument()
                    .add("/Name", Collections.singletonMap("City", "Netanya"))
                    .remove("/Name"));

            assertThat(loadDocument(store).has("Name")).isFalse();
        }
    }

    @Test
    public void patchingWithTestAsSingleOperation() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'The Wall'}");

            JsonPatchDocument jpd = new JsonPatchDocument().test("/Name", "The Wal");

            assertThatThrownBy(() -> send(store, jpd))
                    .isInstanceOf(RavenException.class)
                    .hasMessageContaining("System.InvalidOperationException: The current value 'The Wall' is not equal to the test value 'The Wal'");
        }
    }

    @Test
    public void patchingWithTestMultipleTests() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("Float", 13f);
            values.put("Double", 13d);
            values.put("Decimal", new BigDecimal(13));
            values.put("Long", 13L);
            values.put("String", "The Wall");
            values.put("Boolean1", false);
            values.put("Boolean2", true);
            values.put("Contact", Collections.singletonMap("Name", "Stav"));

            Map<String, Object> document = new LinkedHashMap<>(values);
            document.put("List", values);
            putDocument(store, store.getConventions().getEntityMapper().valueToTree(document));

            JsonPatchDocument jpd = new JsonPatchDocument().test("/List", values);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                jpd.test("/" + entry.getKey(), entry.getValue());
            }

            JsonPatchResult result = send(store, jpd);
            assertThat(result.getStatus()).isNotNull();
        }
    }

    @Test
    public void patchingWithTestAsSingleOperationNumericVsString() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'1'}");

            JsonPatchDocument jpd = new JsonPatchDocument().test("/Name", 1);

            assertThatThrownBy(() -> send(store, jpd))
                    .isInstanceOf(RavenException.class)
                    .hasMessageContaining("System.InvalidOperationException: The current value '1' is not equal to the test value '1'");
        }
    }

    @Test
    public void patchingWithTestMultipleOperations() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'Hibernating','MyArray':[1,2,5]}");

            List<Object> expectedArray = Arrays.asList(1, Collections.singletonMap("City", "Hadera"), 5);

            send(store, new JsonPatchDocument()
                    .replace("/MyArray/1", Collections.singletonMap("City", "Netanya"))
                    .replace("/MyArray/1/City", "Hadera")
                    .test("/MyArray", expectedArray));

            assertDocument(store, "{'Name':'Hibernating','MyArray':[1,{'City':'Hadera'},5]}");
        }
    }

    @Test
    public void patchingWithTestMultipleOperationsFailure() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'Hibernating','MyArray':[1,2,5]}");

            List<Object> staleArray = Arrays.asList(1, Collections.singletonMap("City", "Netanya"), 5);

            JsonPatchDocument jpd = new JsonPatchDocument()
                    .replace("/MyArray/1", Collections.singletonMap("City", "Netanya"))
                    .replace("/MyArray/1/City", "Hadera")
                    .test("/MyArray", staleArray);

            assertThatThrownBy(() -> send(store, jpd))
                    .isInstanceOf(RavenException.class)
                    .hasMessageContaining("The current value '[1,{\"City\":\"Hadera\"},5]' is not equal to the test value '[1,{\"City\":\"Netanya\"},5]");

            // operations must not be applied when a test fails
            assertDocument(store, "{'Name':'Hibernating','MyArray':[1,2,5]}");
        }
    }

    @Test
    public void patchingWithDeferSimpleAdd() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                NamedObject named = new NamedObject();
                named.setName("Hibernating");
                session.store(named, DOC_ID);
                session.saveChanges();

                session.advanced().defer(new JsonPatchCommandData(DOC_ID,
                        new JsonPatchDocument().add("/name", Collections.singletonMap("City", "Netanya"))));
                session.saveChanges();

                // the tracked entity is refreshed from the modified document
                assertThat(named.getName()).isEqualTo(Collections.singletonMap("City", "Netanya"));
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(NamedObject.class, DOC_ID).getName())
                        .isEqualTo(Collections.singletonMap("City", "Netanya"));
            }
        }
    }

    @Test
    public void patchingWithDeferInsertToArray() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                NamedObject named = new NamedObject();
                named.setName("Hibernating");
                named.setMyArray(new ArrayList<>(Arrays.asList(1, 2, 3)));
                session.store(named, DOC_ID);
                session.saveChanges();

                session.advanced().defer(new JsonPatchCommandData(DOC_ID,
                        new JsonPatchDocument().add("/myArray/2", Collections.singletonMap("City", "Netanya"))));
                session.saveChanges();

                assertThat(named.getMyArray()).containsExactly(1, 2, Collections.singletonMap("City", "Netanya"), 3);
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(NamedObject.class, DOC_ID).getMyArray())
                        .containsExactly(1, 2, Collections.singletonMap("City", "Netanya"), 3);
            }
        }
    }

    // coverage beyond the C# suite: copy, and the remaining operations through a deferred command

    @Test
    public void patchingWithCopy() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'Hibernating','Address':{'City':'Hadera'},'MyArray':[1,2]}");

            send(store, new JsonPatchDocument()
                    .copy("/Name", "/Alias")
                    .copy("/Address", "/Shipping")
                    .copy("/MyArray/0", "/MyArray/-"));

            assertDocument(store, "{'Name':'Hibernating','Address':{'City':'Hadera'},'MyArray':[1,2,1]," +
                    "'Alias':'Hibernating','Shipping':{'City':'Hadera'}}");
        }
    }

    @Test
    public void patchingWithCopyFromNonExistentPathFails() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'Hibernating'}");

            assertThatThrownBy(() -> send(store, new JsonPatchDocument().copy("/Missing", "/Alias")))
                    .isInstanceOf(RavenException.class);

            assertDocument(store, "{'Name':'Hibernating'}");
        }
    }

    @Test
    public void patchingWithDeferMoveCopyTest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            putDocument(store, "{'Name':'Hibernating','City':'Hadera'}");

            try (IDocumentSession session = store.openSession()) {
                session.advanced().defer(new JsonPatchCommandData(DOC_ID, new JsonPatchDocument()
                        .test("/Name", "Hibernating")
                        .move("/City", "/Town")
                        .copy("/Name", "/Alias")));
                session.saveChanges();
            }

            assertDocument(store, "{'Name':'Hibernating','Town':'Hadera','Alias':'Hibernating'}");
        }
    }

    @Test
    public void operationsAreSerializedPerRfc6902() throws Exception {
        JsonPatchDocument jpd = new JsonPatchDocument()
                .add("/a", 1)
                .add("/n", null)
                .remove("/b")
                .replace("/c", "x")
                .move("/d", "/e")
                .copy("/f", "/g")
                .test("/h", true);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator generator = MAPPER.getFactory().createGenerator(output)) {
            jpd.serialize(generator, JsonExtensions.getDefaultEntityMapper());
        }

        JsonNode expected = MAPPER.readTree(("{'Operations':[" +
                "{'op':'add','path':'/a','value':1}," +
                "{'op':'add','path':'/n','value':null}," +
                "{'op':'remove','path':'/b'}," +
                "{'op':'replace','path':'/c','value':'x'}," +
                "{'op':'move','path':'/e','from':'/d'}," +
                "{'op':'copy','path':'/g','from':'/f'}," +
                "{'op':'test','path':'/h','value':true}]}").replace('\'', '"'));

        assertThat(MAPPER.readTree(output.toByteArray())).isEqualTo(expected);
    }

    private static JsonPatchResult send(IDocumentStore store, JsonPatchDocument jpd) {
        return store.operations().send(new JsonPatchOperation(DOC_ID, jpd));
    }

    private static void putDocument(IDocumentStore store, String json) throws Exception {
        putDocument(store, (ObjectNode) MAPPER.readTree(json.replace('\'', '"')));
    }

    private static void putDocument(IDocumentStore store, ObjectNode document) {
        store.getRequestExecutor().execute(new PutDocumentCommand(store.getConventions(), DOC_ID, null, document));
    }

    private static ObjectNode loadDocument(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            ObjectNode document = session.load(ObjectNode.class, DOC_ID);
            document.remove("@metadata");
            return document;
        }
    }

    private static void assertDocument(IDocumentStore store, String expectedJson) throws Exception {
        assertThat(loadDocument(store)).isEqualTo(MAPPER.readTree(expectedJson.replace('\'', '"')));
    }

    public static class NamedObject {
        private String id;
        private Object name;
        private List<Object> myArray;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Object getName() {
            return name;
        }

        public void setName(Object name) {
            this.name = name;
        }

        public List<Object> getMyArray() {
            return myArray;
        }

        public void setMyArray(List<Object> myArray) {
            this.myArray = myArray;
        }
    }
}
