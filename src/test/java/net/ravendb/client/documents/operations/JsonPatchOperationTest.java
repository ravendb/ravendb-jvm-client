package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetDocumentsCommand;
import net.ravendb.client.documents.commands.batches.JsonPatchCommandData;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.infrastructure.orders.Company;
import net.ravendb.client.infrastructure.orders.Contact;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPatchOperationTest extends RemoteTestBase {
    @Test
    public void patchingWithAdd() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId;

            Map<String, Object> originalCompany = new LinkedHashMap<>();
            originalCompany.put("Name", "The Wall");

            try (IDocumentSession session = store.openSession()) {
                session.store(originalCompany);
                documentId = session.advanced().getDocumentId(originalCompany);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            jpd.add(new JsonPointer( "/Name"), mapper.readTree("\"Hibernating Rhinos\""));

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dbCompany = session.load(Map.class, documentId);

                dbCompany.remove("@metadata");

                JsonNode originalNode = mapper.valueToTree(originalCompany);
                JsonNode patchedLocal = jpd.apply(originalNode);

                JsonNode patchedServer = mapper.valueToTree(dbCompany);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithEscaping() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId;

            Map<String, Object> originalCompany = new LinkedHashMap<>();
            originalCompany.put("~", "~");
            originalCompany.put("/", "/");
            originalCompany.put("foo/bar~", "foo/bar~");

            List<Map<String, Object>> biscuits = new ArrayList<>();

            Map<String, Object> prop1 = new LinkedHashMap<>();
            prop1.put("~", "Nested~");
            biscuits.add(prop1);

            Map<String, Object> prop2 = new LinkedHashMap<>();
            prop2.put("/", "Nested/");
            biscuits.add(prop2);

            Map<String, Object> prop3 = new LinkedHashMap<>();
            prop3.put("foo/bar~", "NestedFoo/Bar~");
            biscuits.add(prop3);

            originalCompany.put("biscuits", biscuits);

            try (IDocumentSession session = store.openSession()) {
                session.store(originalCompany);
                documentId = session.advanced().getDocumentId(originalCompany);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            jpd.add(new JsonPointer("/~0"), mapper.readTree("\"Hibernating Rhinos1\""));
            jpd.replace(new JsonPointer("/~1"), mapper.readTree("\"Hibernating Rhinos2\""));
            jpd.add(new JsonPointer("/foo~1bar~0"), mapper.readTree("\"Hibernating Rhinos3\""));

            jpd.add(new JsonPointer("/biscuits/0/~0"), mapper.readTree("\"Hibernating Rhinos1\""));
            jpd.add(new JsonPointer("/biscuits/1/~1"), mapper.readTree("\"Hibernating Rhinos2\""));
            jpd.replace(new JsonPointer("/biscuits/1/~1"), mapper.readTree("\"Hibernating Rhinos replaced\""));
            jpd.add(new JsonPointer("/biscuits/2/foo~1bar~0"), mapper.readTree("\"Hibernating Rhinos3\""));

            JsonPatchResult result = store.operations().send(new JsonPatchOperation(documentId, jpd));
            assertEquals(PatchStatus.PATCHED, result.getStatus());

            try (IDocumentSession session = store.openSession()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dbCompany = session.load(Map.class, documentId);
                dbCompany.remove("@metadata");
                JsonNode originalNode = mapper.valueToTree(originalCompany);
                JsonNode patchedLocal = jpd.apply(originalNode);
                JsonNode patchedServer = mapper.valueToTree(dbCompany);
                assertEquals(patchedLocal, patchedServer);
            }
        }
    }


    @Test
    public void shouldPatchDocumentWithCamelCase() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                MyUser user = new MyUser();
                user.setUserName("john");
                user.setAge(10);

                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {

                MyUser user = session.query(MyUser.class).single();
                JsonPatchDocument patchDoc = new JsonPatchDocument();

                ObjectNode analyticsNode = new ObjectMapper().createObjectNode();
                analyticsNode.put("visits", 32);

                patchDoc.add(new JsonPointer("/analytics"), analyticsNode);

                store.operations().send(new JsonPatchOperation("users/1", patchDoc));
                session.advanced().patch(user, "age", 21);

                session.saveChanges();
            }

            GetDocumentsCommand getDocs = new GetDocumentsCommand(store.getConventions(), "users/1", null, false);
            store.getRequestExecutor().execute(getDocs);
            JsonNode res = getDocs.getResult().getResults().get(0);

            String json = res.toString();
            assertTrue(json.contains("\"visits\":32"));
            assertTrue(json.contains("\"age\":21"));
        }
    }

    @Test
    public void patchingWithAddNonExistentDocId() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId;

            MyUser originalUser = new MyUser();
            originalUser.setUserName("The Wall");

            try (IDocumentSession session = store.openSession()) {
                session.store(originalUser);
                documentId = session.advanced().getDocumentId(originalUser);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            jpd.add(new JsonPointer("/Name"), mapper.readTree("\"Hibernating Rhinos\""));
            String nonExistentId = documentId + "1";

            RavenException error = assertThrows(
                    RavenException.class,
                    () -> store.operations().send(new JsonPatchOperation(nonExistentId, jpd))
            );

            assertTrue(
                    error.getMessage().contains("does not exist"),
                    "Expected error message to mention missing document"
            );
        }
    }

    @Test
    public void patchingWithReplaceNestedPathEscaping() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId;
            Map<String, Object> originalCompany = new LinkedHashMap<>();

            Map<String, Object> nested = new LinkedHashMap<>();
            nested.put("Name", "Hibernating");

            originalCompany.put("/", nested);

            try (IDocumentSession session = store.openSession()) {
                session.store(originalCompany);
                documentId = session.advanced().getDocumentId(originalCompany);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            jpd.replace(new JsonPointer("/~1/Name"), mapper.readTree("\"Developer\""));

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dbCompany = session.load(Map.class, documentId);
                dbCompany.remove("@metadata");
                JsonNode originalNode = mapper.valueToTree(originalCompany);
                JsonNode patchedLocal = jpd.apply(originalNode);
                JsonNode patchedServer = mapper.valueToTree(dbCompany);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithAddNestedPath() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId;
            Map<String, Object> originalCompany = new LinkedHashMap<>();

            Map<String, Object> contact = new LinkedHashMap<>();
            contact.put("Name", "Hibernating");

            originalCompany.put("Contact", contact);

            try (IDocumentSession session = store.openSession()) {
                session.store(originalCompany);
                documentId = session.advanced().getDocumentId(originalCompany);
                session.saveChanges();
            }
            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            jpd.add(new JsonPointer("/Contact/Title"), mapper.readTree("\"Developer\""));
            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dbCompany = session.load(Map.class, documentId);
                dbCompany.remove("@metadata");
                JsonNode originalNode = mapper.valueToTree(originalCompany);
                JsonNode patchedLocal = jpd.apply(originalNode);
                JsonNode patchedServer = mapper.valueToTree(dbCompany);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithAddNestedPathDoesntExist() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {
            String documentId;

            Map<String, Object> originalCompany = new LinkedHashMap<>();
            Map<String, Object> contact = new LinkedHashMap<>();
            contact.put("City", "Hadera");
            contact.put("LastName", "Rhinos");
            originalCompany.put("Contact", contact);

            try (IDocumentSession session = store.openSession()) {
                session.store(originalCompany);
                documentId = session.advanced().getDocumentId(originalCompany);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.add(new JsonPointer("/Contact/FirstName"), mapper.readTree("\"Hibernating\""));

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                @SuppressWarnings("unchecked")
                Map<String, Object> dbCompany = session.load(Map.class, documentId);
                dbCompany.remove("@metadata");

                JsonNode originalNode = mapper.valueToTree(originalCompany);
                JsonNode patchedLocal = jpd.apply(originalNode);
                JsonNode patchedServer = mapper.valueToTree(dbCompany);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithAddMoreThanOneNonexistentProperty() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Company originalCompany = new Company();
            Contact contact = new Contact();
            contact.setName("Rhinos");
            originalCompany.setContact(contact);

            try (IDocumentSession session = store.openSession()) {
                session.store(originalCompany);
                documentId = session.advanced().getDocumentId(originalCompany);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.add(new JsonPointer("/contact/FirstName/ExtraProp"), mapper.readTree("\"Hiber\""));

            RavenException error = assertThrows(
                    RavenException.class,
                    () -> store.operations().send(new JsonPatchOperation(documentId, jpd))
            );

            assertTrue(
                    error.getMessage().contains("Cannot reach target location")
                            && error.getMessage().contains("Failed to fetch 'FirstName'")
                            && error.getMessage().contains("/contact/FirstName/ExtraProp")
            );
        }
    }

    @Test
    public void patchingWithAddToArrayAtTheEnd() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Map<String, Object> testObject = new LinkedHashMap<>();
            List<Object> list = new ArrayList<>();
            list.add(1);
            list.add(2);
            list.add(4);
            testObject.put("MyArray", list);

            try (IDocumentSession session = store.openSession()) {
                session.store(testObject);
                documentId = session.advanced().getDocumentId(testObject);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.add(new JsonPointer("/MyArray/-"), mapper.readTree("5"));

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                @SuppressWarnings("unchecked")
                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(testObject);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithAddToArrayAtIndex() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Map<String, Object> testObject = new LinkedHashMap<>();
            List<Object> list = new ArrayList<>();
            list.add(1);
            list.add(2);
            list.add(4);
            testObject.put("MyArray", list);

            try (IDocumentSession session = store.openSession()) {
                session.store(testObject);
                documentId = session.advanced().getDocumentId(testObject);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.add(new JsonPointer("/MyArray/2"), mapper.readTree("3"));

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                @SuppressWarnings("unchecked")
                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(testObject);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithAddToNestedArraysAtIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId;

            Map<String, Object> myClass = new LinkedHashMap<>();
            List<Object> innerList = new ArrayList<>();
            innerList.add(22);
            innerList.add(23);

            List<Object> outerList = new ArrayList<>();
            outerList.add(1);
            outerList.add(2);
            outerList.add(innerList);

            myClass.put("MyArray", outerList);

            try (IDocumentSession session = store.openSession()) {
                session.store(myClass);
                documentId = session.advanced().getDocumentId(myClass);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.add(new JsonPointer("/MyArray/2/1"), mapper.readTree("100"));
            JsonNode localNode = mapper.valueToTree(myClass);
            JsonNode patchedLocal = jpd.apply(localNode);

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                @SuppressWarnings("unchecked")
                Map<String, Object> changedClass = session.load(Map.class, documentId);
                changedClass.remove("@metadata");

                JsonNode patchedServer = mapper.valueToTree(changedClass);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithRemove() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId;

            Map<String, Object> testObject = new LinkedHashMap<>();
            testObject.put("Name", "The Wall");
            testObject.put("City", "Hadera");

            try (IDocumentSession session = store.openSession()) {
                session.store(testObject);
                documentId = session.advanced().getDocumentId(testObject);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.remove(new JsonPointer("/Name"));

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                @SuppressWarnings("unchecked")
                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(testObject);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithRemoveArrayElement() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Map<String, Object> testObject = new LinkedHashMap<>();
            List<Object> list = new ArrayList<>();
            list.add(1);
            list.add(2);
            list.add(3);
            testObject.put("MyArray", list);

            try (IDocumentSession session = store.openSession()) {
                session.store(testObject);
                documentId = session.advanced().getDocumentId(testObject);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.remove(new JsonPointer("/MyArray/1"));

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                @SuppressWarnings("unchecked")
                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(testObject);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithReplace() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Map<String, Object> originalCompany = new LinkedHashMap<>();
            originalCompany.put("Name", "The Wall");

            try (IDocumentSession session = store.openSession()) {
                session.store(originalCompany);
                documentId = session.advanced().getDocumentId(originalCompany);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.replace(new JsonPointer("/Name"), mapper.readTree("\"Hibernating Rhinos\""));

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                @SuppressWarnings("unchecked")
                Map<String, Object> company = session.load(Map.class, documentId);
                company.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(originalCompany);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(company);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithReplaceAtNonExistent() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Map<String, Object> originalCompany = new LinkedHashMap<>();
            originalCompany.put("Name", "The Wall");

            Map<String, Object> address = new LinkedHashMap<>();
            address.put("City", "Netanya");
            originalCompany.put("Address", address);

            try (IDocumentSession session = store.openSession()) {
                session.store(originalCompany);
                documentId = session.advanced().getDocumentId(originalCompany);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.replace(new JsonPointer("/Address/NonexistentProperty"), mapper.readTree("\"Hibernating Rhinos\""));

            RavenException error = assertThrows(
                    RavenException.class,
                    () -> store.operations().send(new JsonPatchOperation(documentId, jpd))
            );

            assertTrue(
                    error.getMessage().contains("Cannot reach target location")
                            && error.getMessage().contains("Failed to fetch 'NonexistentProperty'")
                            && error.getMessage().contains("/Address/NonexistentProperty")
            );
        }
    }

    @Test
    public void patchingWithReplaceArrayElement() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            MyUser originalObject = new MyUser();
            originalObject.setMyArray(new ArrayList<>(Arrays.asList(1, 2, 4)));

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                documentId = session.advanced().getDocumentId(originalObject);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.replace(new JsonPointer("/myArray/1"), mapper.readTree("100"));

            String json = mapper.writeValueAsString(originalObject);
            Map<String, Object> obj = mapper.readValue(json, Map.class);

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(obj);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithMove() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            MyUser originalObject = new MyUser();
            originalObject.setUserName("Hibernating");

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                documentId = session.advanced().getDocumentId(originalObject);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.move(new JsonPointer("/userName"), new JsonPointer("/NewPropName"));

            String json = mapper.writeValueAsString(originalObject);
            Map<String, Object> obj = mapper.readValue(json, Map.class);

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(obj);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
                assertEquals(patchedLocal.get("NewPropName").asText(), dbObject.get("NewPropName"));
                assertFalse(dbObject.containsKey("name"));
            }
        }
    }

    @Test
    public void patchingWithMoveToNestedInSameObject() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            MyUser originalObject = new MyUser();
            originalObject.setUserName("Hibernating");

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                documentId = session.advanced().getDocumentId(originalObject);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.move(new JsonPointer("/userName"), new JsonPointer("/userName/CompanyName"));

            String json = mapper.writeValueAsString(originalObject);
            Map<String, Object> obj = mapper.readValue(json, Map.class);

            assertThrows(
                    RavenException.class,
                    () -> store.operations().send(new JsonPatchOperation(documentId, jpd))
            );

            assertThrows(
                    Exception.class,
                    () -> {
                        JsonNode node = mapper.valueToTree(obj);
                        jpd.apply(node);
                    }
            );
        }
    }

    @Test
    public void patchingWithMoveArrayElementFromIndexToIndex() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Map<String, Object> originalObject = new LinkedHashMap<>();
            originalObject.put("MyArray", new ArrayList<>(Arrays.asList(20, 40, 10, 30)));

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                documentId = session.advanced().getDocumentId(originalObject);
                session.saveChanges();
            }
            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            jpd.move(new JsonPointer("/MyArray/0"), new JsonPointer("/MyArray/2"));
            jpd.move(new JsonPointer("/MyArray/0"), new JsonPointer("/MyArray/3"));

            JsonNode localNode = mapper.valueToTree(originalObject);
            JsonNode patchedLocal = jpd.apply(localNode);

            store.operations().send(new JsonPatchOperation(documentId, jpd));
            try (IDocumentSession session = store.openSession()) {
                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithMultipleAddReplace() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Map<String, Object> originalObject = new LinkedHashMap<>();
            originalObject.put("Name", "Hibernating");

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                documentId = session.advanced().getDocumentId(originalObject);
                session.saveChanges();
            }

            Map<String, Object> address = new LinkedHashMap<>();
            address.put("City", "Netanya");

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            jpd.add(new JsonPointer("/Name"), mapper.valueToTree(address));
            jpd.replace(new JsonPointer("/Name/City"), mapper.readTree("\"Hadera\""));

            String json = mapper.writeValueAsString(originalObject);
            Map<String, Object> obj = mapper.readValue(json, Map.class);

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(obj);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithMultipleAddRemoveObjects() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            MyUser originalObject = new MyUser();

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                documentId = session.advanced().getDocumentId(originalObject);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> address = new LinkedHashMap<>();
            address.put("City", "Netanya");

            jpd.add(new JsonPointer("/Name"), mapper.valueToTree(address));
            jpd.remove(new JsonPointer("/Name"));

            String json = mapper.writeValueAsString(originalObject);
            Map<String, Object> obj = mapper.readValue(json, Map.class);

            store.operations().send(new JsonPatchOperation(documentId, jpd));

            try (IDocumentSession session = store.openSession()) {

                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(obj);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
                assertFalse(dbObject.containsKey("Name"));
            }
        }
    }

    @Test
    public void patchingWithTestAsSingleOperation() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Company originalCompany = new Company();
            originalCompany.setName("The Wall");

            try (IDocumentSession session = store.openSession()) {
                session.store(originalCompany);
                documentId = session.advanced().getDocumentId(originalCompany);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            jpd.test(new JsonPointer("/name"), mapper.readTree("\"The Wal\""));

            RavenException error = assertThrows(
                    RavenException.class,
                    () -> store.operations().send(new JsonPatchOperation(documentId, jpd))
            );

            assertTrue(
                    error.getMessage().contains("The current value 'The Wall' is not equal to the test value 'The Wal'")
            );
        }
    }

    @Test
    public void patchingWithTestMultipleTests() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            Map<String, Object> list = new LinkedHashMap<>();
            list.put("Id", null);
            list.put("Float", (float) 13);
            list.put("Double", (double) 13);
            list.put("Decimal", new BigDecimal("13"));
            list.put("Long", 13L);
            list.put("String", "The Wall");
            list.put("Boolean1", false);
            list.put("Boolean2", true);

            Map<String, Object> contact = new LinkedHashMap<>();
            contact.put("Name", "Stav");
            list.put("Contact", contact);

            String documentId;

            Map<String, Object> originalObject = new LinkedHashMap<>(list);
            originalObject.put("List", list);

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                session.saveChanges();
                documentId = (String) originalObject.get("Id");
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            jpd.test(new JsonPointer("/List"), mapper.valueToTree(list));

            for (Map.Entry<String, Object> entry : list.entrySet()) {
                if (!entry.getKey().equals("Id")) {
                    jpd.test(new JsonPointer("/" + entry.getKey()), mapper.valueToTree(entry.getValue()));
                }
            }

            store.operations().send(new JsonPatchOperation(documentId, jpd));
        }
    }

    @Test
    public void patchingWithTestAsSingleOperationNumericVsString() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Company originalCompany = new Company();
            originalCompany.setName("1");

            try (IDocumentSession session = store.openSession()) {
                session.store(originalCompany);
                documentId = session.advanced().getDocumentId(originalCompany);
                session.saveChanges();
            }

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();

            jpd.test(new JsonPointer("/name"), mapper.readTree("1"));

            RavenException error = assertThrows(
                    RavenException.class,
                    () -> store.operations().send(new JsonPatchOperation(documentId, jpd))
            );

            assertTrue(
                    error.getMessage().contains("The current value '1' is not equal to the test value '1'")
            );
        }
    }

    @Test
    public void patchingWithTestMultipleOperations() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            MyUser originalObject = new MyUser();
            originalObject.setUserName("Hibernating");
            originalObject.setMyArray(new ArrayList<>(Arrays.asList(1, 2, 5)));

            SinglePropClass innerArrObject = new SinglePropClass();
            innerArrObject.setCity("Netanya");

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                documentId = session.advanced().getDocumentId(originalObject);
                session.saveChanges();

                ObjectMapper mapper = new ObjectMapper();

                JsonPatchDocument jpdLocal = new JsonPatchDocument();
                JsonPatchDocument jpd = new JsonPatchDocument();

                jpdLocal.replace(new JsonPointer("/myArray/1"), mapper.valueToTree(innerArrObject));
                jpdLocal.replace(new JsonPointer("/myArray/1/city"), mapper.readTree("\"Hadera\""));

                JsonNode localNode = mapper.valueToTree(originalObject);
                JsonNode patchedLocal = jpdLocal.apply(localNode);
                originalObject = mapper.treeToValue(patchedLocal, MyUser.class);

                jpd.replace(new JsonPointer("/myArray/1"), mapper.valueToTree(innerArrObject));
                jpd.replace(new JsonPointer("/myArray/1/city"), mapper.readTree("\"Hadera\""));

                jpd.test(new JsonPointer("/myArray"), mapper.valueToTree(originalObject.getMyArray()));

                store.operations().send(new JsonPatchOperation(documentId, jpd));
            }
        }
    }

    @Test
    public void patchingWithTestMultipleOperationsFailure() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Map<String, Object> originalObject = new LinkedHashMap<>();
            originalObject.put("Name", "Hibernating");
            originalObject.put("MyArray", new ArrayList<>(Arrays.asList(1, 2, 5)));

            SinglePropClass innerArrObject = new SinglePropClass();
            innerArrObject.setCity("Netanya");

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                documentId = session.advanced().getDocumentId(originalObject);
                session.saveChanges();
            }
            ObjectMapper mapper = new ObjectMapper();

            JsonPatchDocument jpdLocal = new JsonPatchDocument();
            JsonPatchDocument jpd = new JsonPatchDocument();

            jpdLocal.replace(new JsonPointer("/MyArray/1"), mapper.valueToTree(innerArrObject));
            JsonNode localNode = mapper.valueToTree(originalObject);
            JsonNode patchedLocal = jpdLocal.apply(localNode);
            Map<String, Object> originalObjectPatched = mapper.treeToValue(patchedLocal, Map.class);

            jpd.replace(new JsonPointer("/MyArray/1"), mapper.valueToTree(innerArrObject));
            jpd.replace(new JsonPointer("/MyArray/1/city"), mapper.readTree("\"Hadera\""));

            jpd.test(new JsonPointer("/MyArray"), mapper.valueToTree(originalObjectPatched.get("MyArray")));

            RavenException error = assertThrows(
                    RavenException.class,
                    () -> store.operations().send(new JsonPatchOperation(documentId, jpd))
            );

            assertTrue(error.getMessage().contains(
                    "The current value '[1,{\"city\":\"Hadera\"},5]' is not equal to the test value '[1,{\"city\":\"Netanya\"},5]'"
            ));
            try (IDocumentSession session = store.openSession()) {
                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                assertEquals(originalObject, dbObject);
            }
        }
    }

    @Test
    public void patchingWithDeferSimpleAdd() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Map<String, Object> originalObject = new LinkedHashMap<>();
            originalObject.put("Name", "Hibernating");

            Map<String, Object> address = new LinkedHashMap<>();
            address.put("City", "Netanya");

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.add(new JsonPointer("/Name"), mapper.valueToTree(address));

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                documentId = session.advanced().getDocumentId(originalObject);
                session.saveChanges();

                session.advanced().defer(new JsonPatchCommandData(documentId, jpd));
                session.saveChanges();
            }
            try (IDocumentSession session = store.openSession()) {
                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(originalObject);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    @Test
    public void patchingWithDeferInsertToArray() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {

            String documentId;

            Map<String, Object> originalObject = new LinkedHashMap<>();
            originalObject.put("Name", "Hibernating");
            originalObject.put("MyArray", new ArrayList<>(Arrays.asList(1, 2, 3)));

            Map<String, Object> originalObject2 = new LinkedHashMap<>();
            originalObject2.put("Name", "Hibernating");
            originalObject2.put("MyArray", new ArrayList<>(Arrays.asList(1, 2, 3)));

            Map<String, Object> address = new LinkedHashMap<>();
            address.put("City", "Netanya");

            JsonPatchDocument jpd = new JsonPatchDocument();
            ObjectMapper mapper = new ObjectMapper();
            jpd.add(new JsonPointer("/MyArray/2"), mapper.valueToTree(address));

            try (IDocumentSession session = store.openSession()) {
                session.store(originalObject);
                documentId = session.advanced().getDocumentId(originalObject);
                session.saveChanges();

                originalObject2.put("Id", documentId);

                session.advanced().defer(new JsonPatchCommandData(documentId, jpd));
                session.saveChanges();
            }
            try (IDocumentSession session = store.openSession()) {
                Map<String, Object> dbObject = session.load(Map.class, documentId);
                dbObject.remove("@metadata");

                JsonNode localNode = mapper.valueToTree(originalObject2);
                JsonNode patchedLocal = jpd.apply(localNode);
                JsonNode patchedServer = mapper.valueToTree(dbObject);

                assertEquals(patchedLocal, patchedServer);
            }
        }
    }

    private class SinglePropClass {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    private static class MyUser {
        private String userName;
        private int age;
        private List<Object> myArray;

        public List<Object> getMyArray() {
            return myArray;
        }

        public void setMyArray(List<Object> myArray) {
            this.myArray = myArray;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
