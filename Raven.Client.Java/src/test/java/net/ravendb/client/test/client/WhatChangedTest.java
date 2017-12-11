package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.DocumentsChanges;
import net.ravendb.client.documents.session.IDocumentSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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

    /* TODO


        [Fact]
        public void What_Changed_Array_Value_Added()
        {
            using (var store = GetDocumentStore())
            {
                using (var newSession = store.OpenSession())
                {
                    newSession.Store(new Arr
                    {
                        Array = new[] {(dynamic)"a", 1, "b"}
                    }, "arr/1");
                    newSession.SaveChanges();
                }

                using (var newSession = store.OpenSession())
                {
                    var arr = newSession.Load<Arr>("arr/1");
                    arr.Array = new[] {(dynamic)"a", 1, "b", "c", 2};

                    var changes = newSession.Advanced.WhatChanged();
                    Assert.Equal(1, changes.Count);
                    Assert.Equal(2, changes["arr/1"].Length);

                    Assert.Equal(DocumentsChanges.ChangeType.ArrayValueAdded, changes["arr/1"][0].Change);
                    Assert.Null(changes["arr/1"][0].FieldOldValue);
                    Assert.Equal("c", changes["arr/1"][0].FieldNewValue.ToString());

                    Assert.Equal(DocumentsChanges.ChangeType.ArrayValueAdded, changes["arr/1"][1].Change);
                    Assert.Null(changes["arr/1"][1].FieldOldValue);
                    Assert.Equal(2L, changes["arr/1"][1].FieldNewValue);
                }
            }
        }

        [Fact]
        public void What_Changed_Array_Value_Removed()
        {
            using (var store = GetDocumentStore())
            {
                using (var newSession = store.OpenSession())
                {
                    newSession.Store(new Arr
                    {
                        Array = new[] { (dynamic)"a", 1, "b" }
                    }, "arr/1");
                    newSession.SaveChanges();
                }

                using (var newSession = store.OpenSession())
                {
                    var arr = newSession.Load<Arr>("arr/1");
                    arr.Array = new[] { (dynamic)"a"};

                    var changes = newSession.Advanced.WhatChanged();
                    Assert.Equal(1, changes.Count);
                    Assert.Equal(2, changes["arr/1"].Length);

                    Assert.Equal(DocumentsChanges.ChangeType.ArrayValueRemoved, changes["arr/1"][0].Change);
                    Assert.Equal(1L, changes["arr/1"][0].FieldOldValue);
                    Assert.Null(changes["arr/1"][0].FieldNewValue);

                    Assert.Equal(DocumentsChanges.ChangeType.ArrayValueRemoved, changes["arr/1"][1].Change);
                    Assert.Equal("b", changes["arr/1"][1].FieldOldValue.ToString());
                    Assert.Null(changes["arr/1"][0].FieldNewValue);
                }
            }
        }

        [Fact]
        public void RavenDB_8169()
        {
            //Test that when old and new values are of different type
            //but have the same value, we consider them unchanged

            using (var store = GetDocumentStore())
            {
                using (var newSession = store.OpenSession())
                {
                    newSession.Store(new Int
                    {
                        Number = 1
                    }, "num/1");

                    newSession.Store(new Double
                    {
                        Number = 2.0
                    }, "num/2");

                    newSession.SaveChanges();
                }

                using (var newSession = store.OpenSession())
                {
                    newSession.Load<Double>("num/1");
                    var changes = newSession.Advanced.WhatChanged();

                    Assert.Equal(0 , changes.Count);
                }

                using (var newSession = store.OpenSession())
                {
                    newSession.Load<Int>("num/2");
                    var changes = newSession.Advanced.WhatChanged();

                    Assert.Equal(0, changes.Count);
                }
            }
        }
    }


     */

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
