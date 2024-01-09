package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.DocumentsChanges;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WhatChangedTest extends RemoteTestBase {

    @Test
    public void whatChangedNewField() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession newSession = store.openSession()) {
                BasicName basicName = new BasicName();
                basicName.setName("Toli");
                newSession.store(basicName, "users/1");

                assertThat(newSession.advanced().whatChanged())
                        .hasSize(1);
                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                NameAndAge user = newSession.load(NameAndAge.class, "users/1");
                user.setAge(5);
                Map<String, List<DocumentsChanges>> changes = newSession.advanced().whatChanged();
                assertThat(changes.get("users/1"))
                        .hasSize(1);

                assertThat(changes.get("users/1").get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.NEW_FIELD);
                newSession.saveChanges();
            }
        }
    }

    @Test
    public void whatChangedRemovedField() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession newSession = store.openSession()) {
                NameAndAge nameAndAge = new NameAndAge();
                nameAndAge.setAge(5);
                nameAndAge.setName("Toli");

                newSession.store(nameAndAge, "users/1");

                assertThat(newSession.advanced().whatChanged())
                        .hasSize(1);
                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                newSession.load(BasicAge.class, "users/1");

                Map<String, List<DocumentsChanges>> changes = newSession.advanced().whatChanged();
                assertThat(changes.get("users/1"))
                        .hasSize(1);

                assertThat(changes.get("users/1").get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.REMOVED_FIELD);
                newSession.saveChanges();
            }
        }
    }


    @Test
    public void whatChangedChangeField() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession newSession = store.openSession()) {
                BasicAge basicAge = new BasicAge();
                basicAge.setAge(5);
                newSession.store(basicAge, "users/1");

                assertThat(newSession.advanced().whatChanged())
                        .hasSize(1);
                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                newSession.load(Int.class, "users/1");

                Map<String, List<DocumentsChanges>> changes = newSession.advanced().whatChanged();
                assertThat(changes.get("users/1"))
                        .hasSize(2);

                assertThat(changes.get("users/1").get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.REMOVED_FIELD);
                assertThat(changes.get("users/1").get(1).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.NEW_FIELD);
                newSession.saveChanges();
            }
        }
    }


    @Test
    public void whatChangedArrayValueChanged() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession newSession = store.openSession()) {
                Arr arr = new Arr();
                arr.setArray(new Object[] {  "a", 1, "b"  });

                newSession.store(arr, "users/1");
                Map<String, List<DocumentsChanges>> changes = newSession.advanced().whatChanged();

                assertThat(changes)
                        .hasSize(1);

                assertThat(changes.get("users/1"))
                        .hasSize(1);
                assertThat(changes.get("users/1").get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.DOCUMENT_ADDED);

                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                Arr arr = newSession.load(Arr.class, "users/1");
                arr.setArray(new Object[] {  "a", 2, "c" });

                Map<String, List<DocumentsChanges>> changes = newSession.advanced().whatChanged();
                assertThat(changes)
                        .hasSize(1);

                assertThat(changes.get("users/1"))
                        .hasSize(2);

                assertThat(changes.get("users/1").get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.ARRAY_VALUE_CHANGED);
                assertThat(changes.get("users/1").get(0).getFieldOldValue().toString())
                        .isEqualTo("1");
                assertThat(changes.get("users/1").get(0).getFieldNewValue())
                        .isEqualTo(2);

                assertThat(changes.get("users/1").get(1).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.ARRAY_VALUE_CHANGED);
                assertThat(changes.get("users/1").get(1).getFieldOldValue().toString())
                        .isEqualTo("\"b\"");
                assertThat(changes.get("users/1").get(1).getFieldNewValue().toString())
                        .isEqualTo("\"c\"");
            }
        }
    }

    @Test
    public void what_Changed_Array_Value_Added() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession newSession = store.openSession()) {
                Arr arr = new Arr();
                arr.setArray(new Object[] { "a", 1, "b"});
                newSession.store(arr, "arr/1");
                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                Arr arr = newSession.load(Arr.class, "arr/1");
                arr.setArray(new Object[]{ "a", 1, "b", "c", 2 });

                Map<String, List<DocumentsChanges>> changes = newSession.advanced().whatChanged();

                assertThat(changes.size())
                        .isEqualTo(1);
                assertThat(changes.get("arr/1"))
                        .hasSize(2);

                assertThat(changes.get("arr/1").get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.ARRAY_VALUE_ADDED);

                assertThat(changes.get("arr/1").get(0).getFieldNewValue().toString())
                        .isEqualTo("\"c\"");
                assertThat(changes.get("arr/1").get(0).getFieldOldValue())
                        .isNull();

                assertThat(changes.get("arr/1").get(1).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.ARRAY_VALUE_ADDED);

                assertThat(changes.get("arr/1").get(1).getFieldNewValue())
                        .isEqualTo(2);

                assertThat(changes.get("arr/1").get(1).getFieldOldValue())
                        .isNull();
            }
        }
    }

    @Test
    public void what_Changed_Array_Value_Removed() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession newSession = store.openSession()) {
                Arr arr = new Arr();
                arr.setArray(new Object[] { "a", 1, "b"});
                newSession.store(arr, "arr/1");
                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                Arr arr = newSession.load(Arr.class, "arr/1");

                arr.setArray(new Object[] { "a" });

                Map<String, List<DocumentsChanges>> changes = newSession.advanced().whatChanged();
                assertThat(changes.size())
                        .isEqualTo(1);
                assertThat(changes.get("arr/1"))
                        .hasSize(2);

                assertThat(changes.get("arr/1").get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.ARRAY_VALUE_REMOVED);
                assertThat(changes.get("arr/1").get(0).getFieldOldValue())
                        .isEqualTo(1);
                assertThat(changes.get("arr/1").get(0).getFieldNewValue())
                        .isNull();

                assertThat(changes.get("arr/1").get(1).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.ARRAY_VALUE_REMOVED);
                assertThat(changes.get("arr/1").get(1).getFieldOldValue().toString())
                        .isEqualTo("\"b\"");
                assertThat(changes.get("arr/1").get(1).getFieldNewValue())
                        .isNull();
            }
        }
    }

    @Test
    public void ravenDB_8169() throws Exception {
        //Test that when old and new values are of different type
        //but have the same value, we consider them unchanged

        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {
                Int anInt = new Int();
                anInt.setNumber(1);

                newSession.store(anInt, "num/1");

                Double aDouble = new Double();
                aDouble.setNumber(2.0);
                newSession.store(aDouble, "num/2");

                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                newSession.load(Double.class, "num/1");
                Map<String, List<DocumentsChanges>> changes = newSession.advanced().whatChanged();
                assertThat(changes)
                        .hasSize(0);
            }

            try (IDocumentSession newSession = store.openSession()) {
                newSession.load(Int.class, "num/2");

                Map<String, List<DocumentsChanges>> changes = newSession.advanced().whatChanged();
                assertThat(changes)
                        .hasSize(0);
            }

        }
    }

    @Test
    public void whatChanged_should_be_idempotent_operation() throws Exception {
        //RavenDB-9150

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("user1");

                User user2 = new User();
                user2.setName("user2");
                user2.setAge(1);

                User user3 = new User();
                user3.setName("user3");
                user3.setAge(1);

                session.store(user1, "users/1");
                session.store(user2, "users/2");
                session.store(user3, "users/3");


                assertThat(session.advanced().whatChanged())
                        .hasSize(3);

                session.saveChanges();

                user1 = session.load(User.class, "users/1");
                user2 = session.load(User.class, "users/2");

                user1.setAge(10);
                session.delete(user2);

                assertThat(session.advanced().whatChanged())
                        .hasSize(2);
                assertThat(session.advanced().whatChanged())
                        .hasSize(2);
            }
        }
    }

    @Test
    public void hasChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("user1");

                User user2 = new User();
                user2.setName("user2");
                user2.setAge(1);

                session.store(user1, "users/1");
                session.store(user2, "users/2");
                session.saveChanges();
            }


            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().hasChanges())
                        .isFalse();

                User u1 = session.load(User.class, "users/1");
                User u2 = session.load(User.class, "users/2");

                assertThat(session.advanced().hasChanged(u1))
                        .isFalse();
                assertThat(session.advanced().hasChanged(u2))
                        .isFalse();

                u1.setName("new name");

                assertThat(session.advanced().hasChanged(u1))
                        .isTrue();
                assertThat(session.advanced().hasChanged(u2))
                        .isFalse();
                assertThat(session.advanced().hasChanges())
                        .isTrue();

            }
        }
    }

    @Test
    public void what_Changed_For_Delete_After_Change_Value() throws Exception {
        //RavenDB-13501

        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                String id = "ABC";
                TestObject o = new TestObject();
                o.setId(id);
                o.setA("A");
                o.setB("A");
                session.store(o);
                session.saveChanges();

                assertThat(session.advanced().hasChanges())
                        .isFalse();

                o = session.load(TestObject.class, id);
                o.setA("B");
                o.setB("C");
                session.delete(o);

                List<DocumentsChanges> whatChangedFor = session.advanced().whatChangedFor(o);
                assertThat(whatChangedFor.size() == 1 && whatChangedFor.get(0).getChange() == DocumentsChanges.ChangeType.DOCUMENT_DELETED)
                        .isTrue();

                session.saveChanges();

                o = session.load(TestObject.class, id);
                assertThat(o)
                        .isNull();

            }
        }
    }

    @Test
    @Disabled("We don't support whatChanged for metadata")
    public void what_Changed_For_RemovingAndAddingSameAmountOfFieldsToObjectShouldWork() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String docId = "d/1";
            try (IDocumentSession session = store.openSession()) {
                Doc d = new Doc();
                session.store(d, docId);
                IMetadataDictionary meta = session.advanced().getMetadataFor(d);

                meta.put("Test-A", new String[] { "a", "a", "a" });
                meta.put("Test-C", new String[] { "c", "c", "c" });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Doc d = session.load(Doc.class, docId);
                IMetadataDictionary meta = session.advanced().getMetadataFor(d);

                meta.put("Test-A", new String[] { "b", "a", "c" });

                List<DocumentsChanges> changes = session.advanced().whatChangedFor(d);
                assertThat(changes)
                        .hasSize(2);
                assertThat(changes.get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.ARRAY_VALUE_CHANGED);
                assertThat(changes.get(0).getFieldName())
                        .isEqualTo("Test-A");
                assertThat(changes.get(1).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.ARRAY_VALUE_CHANGED);
                assertThat(changes.get(1).getFieldName())
                        .isEqualTo("Test-A");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Doc d = session.load(Doc.class, docId);
                IMetadataDictionary meta = session.advanced().getMetadataFor(d);

                meta.remove("Test-A");

                List<DocumentsChanges> changes = session.advanced().whatChangedFor(d);
                assertThat(changes)
                        .hasSize(1);
                assertThat(changes.get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.REMOVED_FIELD);
            }

            try (IDocumentSession session = store.openSession()) {
                Doc d = session.load(Doc.class, docId);
                IMetadataDictionary meta = session.advanced().getMetadataFor(d);

                meta.remove("Test-A");
                meta.remove("Test-C");
                meta.put("Test-B", new String[] { "b", "b", "b" });
                meta.put("Test-D", new String[] { "d", "d", "d" });
                List<DocumentsChanges> changes = session.advanced().whatChangedFor(d);

                assertThat(changes)
                        .hasSize(4);
                assertThat(changes.stream().filter(x -> x.getFieldName().equals("Test-A")).findFirst().get().getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.REMOVED_FIELD);
                assertThat(changes.stream().filter(x -> x.getFieldName().equals("Test-C")).findFirst().get().getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.REMOVED_FIELD);
                assertThat(changes.stream().filter(x -> x.getFieldName().equals("Test-B")).findFirst().get().getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.NEW_FIELD);
                assertThat(changes.stream().filter(x -> x.getFieldName().equals("Test-D")).findFirst().get().getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.NEW_FIELD);
            }

            try (IDocumentSession session = store.openSession()) {
                Doc d = session.load(Doc.class, docId);
                IMetadataDictionary meta = session.advanced().getMetadataFor(d);

                meta.remove("Test-A");
                meta.put("Test-B", new String[] { "b", "b", "b" });

                List<DocumentsChanges> changes = session.advanced().whatChangedFor(d);
                assertThat(changes.stream().filter(x -> x.getFieldName().equals("Test-A")).findFirst().get().getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.REMOVED_FIELD);
                assertThat(changes.stream().filter(x -> x.getFieldName().equals("Test-B")).findFirst().get().getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.NEW_FIELD);
            }
        }
    }

    public static class BasicName {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


    public static class NameAndAge {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class BasicAge {
        private int age;

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class Int {
        private int number;

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }
    }

    public static class Double {
        private double number;

        public double getNumber() {
            return number;
        }

        public void setNumber(double number) {
            this.number = number;
        }
    }


    public static class Arr {
        private Object[] array;

        public Object[] getArray() {
            return array;
        }

        public void setArray(Object[] array) {
            this.array = array;
        }
    }

    public static class TestObject {
        private String id;
        private String a;
        private String b;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }
    }

    public static class Doc {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
