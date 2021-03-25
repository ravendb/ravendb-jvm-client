package net.ravendb.client.test.client;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetDocumentsCommand;
import net.ravendb.client.documents.commands.GetDocumentsResult;
import net.ravendb.client.documents.session.DocumentsChanges;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CrudTest extends RemoteTestBase {




    @Test
    public void crudOperations() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {
                User user1 = new User();
                user1.setLastName("user1");
                newSession.store(user1, "users/1");

                User user2 = new User();
                user2.setName("user2");
                user1.setAge(1);
                newSession.store(user2, "users/2");

                User user3 = new User();
                user3.setName("user3");
                user3.setAge(1);
                newSession.store(user3, "users/3");


                User user4 = new User();
                user4.setName("user4");
                newSession.store(user4, "users/4");

                newSession.delete(user2);
                user3.setAge(3);
                newSession.saveChanges();

                User tempUser = newSession.load(User.class, "users/2");
                assertThat(tempUser)
                        .isNull();

                tempUser = newSession.load(User.class, "users/3");
                assertThat(tempUser.getAge())
                        .isEqualTo(3);

                user1 = newSession.load(User.class, "users/1");
                user4 = newSession.load(User.class, "users/4");

                newSession.delete(user4);
                user1.setAge(10);
                newSession.saveChanges();

                tempUser = newSession.load(User.class, "users/4");
                assertThat(tempUser)
                        .isNull();
                tempUser = newSession.load(User.class, "users/1");
                assertThat(tempUser.getAge())
                        .isEqualTo(10);

            }
        }
    }

    @Test
    public void crudOperationsWithWhatChanged() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {
                User user1 = new User();
                user1.setLastName("user1");
                newSession.store(user1, "users/1");

                User user2 = new User();
                user2.setName("user2");
                user1.setAge(1);
                newSession.store(user2, "users/2");

                User user3 = new User();
                user3.setName("user3");
                user3.setAge(1);
                newSession.store(user3, "users/3");


                User user4 = new User();
                user4.setName("user4");
                newSession.store(user4, "users/4");

                newSession.delete(user2);
                user3.setAge(3);

                assertThat(newSession.advanced().whatChanged())
                        .hasSize(4);
                newSession.saveChanges();

                User tempUser = newSession.load(User.class, "users/2");
                assertThat(tempUser)
                        .isNull();
                tempUser = newSession.load(User.class, "users/3");
                assertThat(tempUser.getAge())
                        .isEqualTo(3);

                user1 = newSession.load(User.class, "users/1");
                user4 = newSession.load(User.class, "users/4");

                newSession.delete(user4);
                user1.setAge(10);
                assertThat(newSession.advanced().whatChanged())
                        .hasSize(2);

                newSession.saveChanges();

                tempUser = newSession.load(User.class, "users/4");
                assertThat(tempUser)
                        .isNull();

                tempUser = newSession.load(User.class, "users/1");
                assertThat(tempUser.getAge())
                        .isEqualTo(10);
            }
        }
    }

    @Test
    public void crudOperationsWithArrayInObject() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {

                Family family = new Family();
                family.setNames(new String[]{ "Hibernating Rhinos", "RavenDB" });
                newSession.store(family, "family/1");
                newSession.saveChanges();

                Family newFamily = newSession.load(Family.class, "family/1");
                newFamily.setNames(new String[]{  "Toli", "Mitzi", "Boki" });
                assertThat(newSession.advanced().whatChanged())
                        .hasSize(1);
                newSession.saveChanges();
            }
        }
    }

    @Test
    public void crudOperationsWithArrayInObject2() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {

                Family family = new Family();
                family.setNames(new String[]{ "Hibernating Rhinos", "RavenDB" });
                newSession.store(family, "family/1");
                newSession.saveChanges();

                Family newFamily = newSession.load(Family.class, "family/1");
                newFamily.setNames(new String[]{ "Hibernating Rhinos", "RavenDB" });
                assertThat(newSession.advanced().whatChanged())
                        .hasSize(0);

                newFamily.setNames(new String[]{ "RavenDB", "Hibernating Rhinos" });
                assertThat(newSession.advanced().whatChanged())
                        .hasSize(1);

                newSession.saveChanges();
            }
        }
    }

    @Test
    public void crudOperationsWithArrayInObject3() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {

                Family family = new Family();
                family.setNames(new String[]{"Hibernating Rhinos", "RavenDB"});
                newSession.store(family, "family/1");
                newSession.saveChanges();

                Family newFamily = newSession.load(Family.class, "family/1");
                newFamily.setNames(new String[] { "RavenDB" });
                assertThat(newSession.advanced().whatChanged())
                        .hasSize(1);
                newSession.saveChanges();
            }
        }
    }

    @Test
    public void crudOperationsWithArrayInObject4() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {

                Family family = new Family();
                family.setNames(new String[]{"Hibernating Rhinos", "RavenDB"});
                newSession.store(family, "family/1");
                newSession.saveChanges();

                Family newFamily = newSession.load(Family.class, "family/1");
                newFamily.setNames(new String[] {  "RavenDB", "Hibernating Rhinos", "Toli", "Mitzi", "Boki" });
                assertThat(newSession.advanced().whatChanged())
                        .hasSize(1);
                newSession.saveChanges();
            }
        }
    }

    @Test
    public void crudOperationsWithNull() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {
                User user = new User();
                user.setName(null);

                newSession.store(user, "users/1");
                newSession.saveChanges();

                User user2 = newSession.load(User.class, "users/1");
                assertThat(newSession.advanced().whatChanged())
                        .isEmpty();

                user2.setAge(3);

                assertThat(newSession.advanced().whatChanged())
                        .hasSize(1);
            }
        }
    }

    public static class Family {
        private String[] names;

        public String[] getNames() {
            return names;
        }

        public void setNames(String[] names) {
            this.names = names;
        }
    }

    public static class FamilyMembers {
        private Member[] members;

        public Member[] getMembers() {
            return members;
        }

        public void setMembers(Member[] members) {
            this.members = members;
        }
    }

    public static class Member {
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

    @Test
    public void crudOperationsWithArrayOfObjects() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {
                Member member1 = new Member();
                member1.setName("Hibernating Rhinos");
                member1.setAge(8);

                Member member2 = new Member();
                member2.setName("RavenDB");
                member2.setAge(4);

                FamilyMembers family = new FamilyMembers();
                family.setMembers(new Member[] { member1, member2 });

                newSession.store(family, "family/1");
                newSession.saveChanges();

                member1 = new Member();
                member1.setName("RavenDB");
                member1.setAge(4);

                member2 = new Member();
                member2.setName("Hibernating Rhinos");
                member2.setAge(8);

                FamilyMembers newFamily = newSession.load(FamilyMembers.class, "family/1");
                newFamily.setMembers(new Member[]{ member1, member2 });

                Map<String, List<DocumentsChanges>> changes = newSession.advanced().whatChanged();
                assertThat(changes)
                        .hasSize(1);

                assertThat(changes.get("family/1"))
                        .hasSize(4);

                assertThat(changes.get("family/1").get(0).getFieldName())
                        .isEqualTo("name");
                assertThat(changes.get("family/1").get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get("family/1").get(0).getFieldOldValue().toString())
                        .isEqualTo("\"Hibernating Rhinos\"");
                assertThat(changes.get("family/1").get(0).getFieldNewValue().toString())
                        .isEqualTo("\"RavenDB\"");

                assertThat(changes.get("family/1").get(1).getFieldName())
                        .isEqualTo("age");
                assertThat(changes.get("family/1").get(1).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get("family/1").get(1).getFieldOldValue().toString())
                        .isEqualTo("8");
                assertThat(changes.get("family/1").get(1).getFieldNewValue().toString())
                        .isEqualTo("4");

                assertThat(changes.get("family/1").get(2).getFieldName())
                        .isEqualTo("name");
                assertThat(changes.get("family/1").get(2).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get("family/1").get(2).getFieldOldValue().toString())
                        .isEqualTo("\"RavenDB\"");
                assertThat(changes.get("family/1").get(2).getFieldNewValue().toString())
                        .isEqualTo("\"Hibernating Rhinos\"");

                assertThat(changes.get("family/1").get(3).getFieldName())
                        .isEqualTo("age");
                assertThat(changes.get("family/1").get(3).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get("family/1").get(3).getFieldOldValue().toString())
                        .isEqualTo("4");
                assertThat(changes.get("family/1").get(3).getFieldNewValue().toString())
                        .isEqualTo("8");


                member1 = new Member();
                member1.setName("Toli");
                member1.setAge(5);

                member2 = new Member();
                member2.setName("Boki");
                member2.setAge(15);

                newFamily.setMembers(new Member[]{ member1, member2 });

                changes = newSession.advanced().whatChanged();
                assertThat(changes)
                        .hasSize(1);

                assertThat(changes.get("family/1"))
                        .hasSize(4);

                assertThat(changes.get("family/1").get(0).getFieldName())
                        .isEqualTo("name");
                assertThat(changes.get("family/1").get(0).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get("family/1").get(0).getFieldOldValue().toString())
                        .isEqualTo("\"Hibernating Rhinos\"");
                assertThat(changes.get("family/1").get(0).getFieldNewValue().toString())
                        .isEqualTo("\"Toli\"");

                assertThat(changes.get("family/1").get(1).getFieldName())
                        .isEqualTo("age");
                assertThat(changes.get("family/1").get(1).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get("family/1").get(1).getFieldOldValue().toString())
                        .isEqualTo("8");
                assertThat(changes.get("family/1").get(1).getFieldNewValue().toString())
                        .isEqualTo("5");

                assertThat(changes.get("family/1").get(2).getFieldName())
                        .isEqualTo("name");
                assertThat(changes.get("family/1").get(2).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get("family/1").get(2).getFieldOldValue().toString())
                        .isEqualTo("\"RavenDB\"");
                assertThat(changes.get("family/1").get(2).getFieldNewValue().toString())
                        .isEqualTo("\"Boki\"");

                assertThat(changes.get("family/1").get(3).getFieldName())
                        .isEqualTo("age");
                assertThat(changes.get("family/1").get(3).getChange())
                        .isEqualTo(DocumentsChanges.ChangeType.FIELD_CHANGED);
                assertThat(changes.get("family/1").get(3).getFieldOldValue().toString())
                        .isEqualTo("4");
                assertThat(changes.get("family/1").get(3).getFieldNewValue().toString())
                        .isEqualTo("15");
            }
        }
    }

    public static class Arr1 {
        private String[] str;

        public String[] getStr() {
            return str;
        }

        public void setStr(String[] str) {
            this.str = str;
        }
    }

    public static class Arr2 {
        private Arr1[] arr1;

        public Arr1[] getArr1() {
            return arr1;
        }

        public void setArr1(Arr1[] arr1) {
            this.arr1 = arr1;
        }
    }

    @Test
    public void crudOperationsWithArrayOfArrays() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {

                Arr1 a1 = new Arr1();
                a1.setStr(new String[]{ "a", "b" });

                Arr1 a2 = new Arr1();
                a2.setStr(new String[]{ "c", "d" });

                Arr2 arr = new Arr2();
                arr.setArr1(new Arr1[]{ a1, a2 });

                newSession.store(arr, "arr/1");
                newSession.saveChanges();

                Arr2 newArr = newSession.load(Arr2.class, "arr/1");

                a1 = new Arr1();
                a1.setStr(new String[]{ "d", "c" });

                a2 = new Arr1();
                a2.setStr(new String[]{ "a", "b" });

                newArr.setArr1(new Arr1[]{ a1, a2 });

                Map<String, List<DocumentsChanges>> whatChanged = newSession.advanced().whatChanged();
                assertThat(whatChanged)
                        .hasSize(1);

                List<DocumentsChanges> change = whatChanged.get("arr/1");
                assertThat(change)
                        .hasSize(4);


                assertThat(change.get(0).getFieldOldValue().toString())
                        .isEqualTo("\"a\"");
                assertThat(change.get(0).getFieldNewValue().toString())
                        .isEqualTo("\"d\"");

                assertThat(change.get(1).getFieldOldValue().toString())
                        .isEqualTo("\"b\"");
                assertThat(change.get(1).getFieldNewValue().toString())
                        .isEqualTo("\"c\"");

                assertThat(change.get(2).getFieldOldValue().toString())
                        .isEqualTo("\"c\"");
                assertThat(change.get(2).getFieldNewValue().toString())
                        .isEqualTo("\"a\"");

                assertThat(change.get(3).getFieldOldValue().toString())
                        .isEqualTo("\"d\"");
                assertThat(change.get(3).getFieldNewValue().toString())
                        .isEqualTo("\"b\"");

                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                Arr2 newArr = newSession.load(Arr2.class, "arr/1");
                Arr1 a1 = new Arr1();
                a1.setStr(new String[]{ "q", "w" });

                Arr1 a2 = new Arr1();
                a2.setStr(new String[]{ "a", "b" });

                newArr.setArr1(new Arr1[]{ a1, a2 });

                Map<String, List<DocumentsChanges>> whatChanged = newSession.advanced().whatChanged();
                assertThat(whatChanged)
                        .hasSize(1);

                List<DocumentsChanges> change = whatChanged.get("arr/1");
                assertThat(change)
                        .hasSize(2);

                assertThat(change.get(0).getFieldOldValue().toString())
                        .isEqualTo("\"d\"");
                assertThat(change.get(0).getFieldNewValue().toString())
                        .isEqualTo("\"q\"");

                assertThat(change.get(1).getFieldOldValue().toString())
                        .isEqualTo("\"c\"");
                assertThat(change.get(1).getFieldNewValue().toString())
                        .isEqualTo("\"w\"");

                newSession.saveChanges();
            }
        }
    }

    @Test
    public void crudCanUpdatePropertyToNull() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {
                User user = new User();
                user.setName("user1");

                newSession.store(user, "users/1");
                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                User user = newSession.load(User.class, "users/1");
                user.setName(null);
                newSession.saveChanges();
            }

            try (IDocumentSession newSession = store.openSession()) {
                User user = newSession.load(User.class, "users/1");
                assertThat(user.getName())
                        .isNull();
            }
        }
    }

    @Test
    public void crudCanUpdatePropertyFromNullToObject() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                Poc poc = new Poc();
                poc.setName("aviv");
                poc.setObj(null);

                session.store(poc, "pocs/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {

                Poc poc = session.load(Poc.class, "pocs/1");
                assertThat(poc.getObj())
                        .isNull();

                User user = new User();
                poc.setObj(user);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Poc poc = session.load(Poc.class, "pocs/1");
                assertThat(poc)
                        .isNotNull();
            }
        }
    }

    public static class Poc {
        private String name;
        private User obj;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public User getObj() {
            return obj;
        }

        public void setObj(User obj) {
            this.obj = obj;
        }
    }
}
