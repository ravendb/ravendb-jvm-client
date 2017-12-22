package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.DocumentsChanges;
import net.ravendb.client.documents.session.IDocumentSession;
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

    //TBD public void What_Changed_Array_Value_Added()
    //TBD public void What_Changed_Array_Value_Removed()
    //TBD public void RavenDB_8169()

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
}
