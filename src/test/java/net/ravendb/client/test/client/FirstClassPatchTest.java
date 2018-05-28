package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class FirstClassPatchTest extends RemoteTestBase {

    private final String _docId = "users/1-A";

    public static class User {
        private Stuff[] stuff;
        private Date lastLogin;
        private int[] numbers;

        public Stuff[] getStuff() {
            return stuff;
        }

        public void setStuff(Stuff[] stuff) {
            this.stuff = stuff;
        }

        public Date getLastLogin() {
            return lastLogin;
        }

        public void setLastLogin(Date lastLogin) {
            this.lastLogin = lastLogin;
        }

        public int[] getNumbers() {
            return numbers;
        }

        public void setNumbers(int[] numbers) {
            this.numbers = numbers;
        }
    }

    public static class Stuff {
        private int key;
        private String phone;
        private Pet pet;
        private Friend friend;
        private Map<String, String> dic;

        public int getKey() {
            return key;
        }

        public void setKey(int key) {
            this.key = key;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Pet getPet() {
            return pet;
        }

        public void setPet(Pet pet) {
            this.pet = pet;
        }

        public Friend getFriend() {
            return friend;
        }

        public void setFriend(Friend friend) {
            this.friend = friend;
        }

        public Map<String, String> getDic() {
            return dic;
        }

        public void setDic(Map<String, String> dic) {
            this.dic = dic;
        }
    }

    public static class Friend {
        private String name;
        private int age;
        private Pet pet;

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

        public Pet getPet() {
            return pet;
        }

        public void setPet(Pet pet) {
            this.pet = pet;
        }
    }

    public static class Pet {
        private String name;
        private String kind;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }
    }


    @Test
    public void canPatch() throws Exception {
        Stuff[] stuff = new Stuff[3];
        stuff[0] = new Stuff();
        stuff[0].setKey(6);

        User user = new User();
        user.setNumbers(new int[] { 66 });
        user.setStuff(stuff);

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(user);
                session.saveChanges();
            }

            Date now = new Date();

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch(_docId, "numbers[0]", 31);
                session.advanced().patch(_docId, "lastLogin", now);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);
                assertThat(loaded.getNumbers()[0])
                        .isEqualTo(31);
                assertThat(loaded.getLastLogin())
                        .isEqualTo(now);

                session.advanced().patch(loaded, "stuff[0].phone", "123456");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);
                assertThat(loaded.getStuff()[0].getPhone())
                        .isEqualTo("123456");
            }
        }
    }


    @Test
    public void canPatchAndModify() throws Exception {
        User user = new User();
        user.setNumbers(new int[] { 66 });

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(user);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);
                loaded.getNumbers()[0] = 1;
                session.advanced().patch(loaded, "numbers[0]", 2);

                assertThatThrownBy(() -> session.saveChanges()).isExactlyInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    public void canPatchComplex() throws Exception {
        Stuff[] stuff = new Stuff[3];
        stuff[0] = new Stuff();
        stuff[0].setKey(6);

        User user = new User();
        user.setStuff(stuff);

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(user);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Stuff newStuff = new Stuff();
                newStuff.setKey(4);
                newStuff.setPhone("9255864406");
                newStuff.setFriend(new Friend());
                session.advanced().patch(_docId, "stuff[1]", newStuff);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);

                assertThat(loaded.getStuff()[1].getPhone())
                        .isEqualTo("9255864406");
                assertThat(loaded.getStuff()[1].getKey())
                        .isEqualTo(4);
                assertThat(loaded.getStuff()[1].getFriend())
                        .isNotNull();

                Pet pet1 = new Pet();
                pet1.setKind("Dog");
                pet1.setName("Hanan");

                Pet friendsPet = new Pet();
                friendsPet.setName("Miriam");
                friendsPet.setKind("Cat");

                Friend friend = new Friend();
                friend.setName("Gonras");
                friend.setAge(28);
                friend.setPet(friendsPet);

                Stuff secondStuff = new Stuff();
                secondStuff.setKey(4);
                secondStuff.setPhone("9255864406");
                secondStuff.setPet(pet1);
                secondStuff.setFriend(friend);

                Map<String, String> map = new HashMap<>();
                map.put("Ohio", "Columbus");
                map.put("Utah", "Salt Lake City");
                map.put("Texas", "Austin");
                map.put("California", "Sacramento");

                secondStuff.setDic(map);

                session.advanced().patch(loaded, "stuff[2]", secondStuff);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);

                assertThat(loaded.getStuff()[2].getPet().getName())
                        .isEqualTo("Hanan");

                assertThat(loaded.getStuff()[2].getFriend().getName())
                        .isEqualTo("Gonras");

                assertThat(loaded.getStuff()[2].getFriend().getPet().getName())
                        .isEqualTo("Miriam");

                assertThat(loaded.getStuff()[2].getDic())
                        .hasSize(4);

                assertThat(loaded.getStuff()[2].getDic().get("Utah"))
                        .isEqualTo("Salt Lake City");
            }
        }
    }

    @Test
    public void canAddToArray() throws Exception {
        Stuff[] stuff = new Stuff[1];

        stuff[0] = new Stuff();
        stuff[0].setKey(6);

        User user = new User();
        user.setStuff(stuff);
        user.setNumbers(new int[] { 1, 2 });

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(user);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                //push
                session.advanced().patch(_docId, "numbers", roles -> roles.add(3));
                session.advanced().patch(_docId, "stuff", roles -> {
                    Stuff stuff1 = new Stuff();
                    stuff1.setKey(75);
                    roles.add(stuff1);
                });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);

                assertThat(loaded.getNumbers()[2])
                        .isEqualTo(3);
                assertThat(loaded.getStuff()[1].getKey())
                        .isEqualTo(75);

                //concat

                session.advanced().patch(loaded, "numbers", roles -> roles.add(101, 102, 103));
                session.advanced().patch(loaded, "stuff", roles -> {
                    Stuff s1 = new Stuff();
                    s1.setKey(102);

                    Stuff s2 = new Stuff();
                    s2.setPhone("123456");

                    roles.add(s1).add(s2);
                });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);
                assertThat(loaded.getNumbers())
                        .hasSize(6);
                assertThat(loaded.getNumbers()[5])
                        .isEqualTo(103);

                assertThat(loaded.getStuff()[2].getKey())
                        .isEqualTo(102);
                assertThat(loaded.getStuff()[3].getPhone())
                        .isEqualTo("123456");

                session.advanced().patch(loaded, "numbers", roles -> roles.add(Arrays.asList(201, 202, 203)));
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);
                assertThat(loaded.getNumbers())
                        .hasSize(9);
                assertThat(loaded.getNumbers()[7])
                        .isEqualTo(202);

            }
        }
    }

    @Test
    public void canRemoveFromArray() throws Exception {
        Stuff[] stuff = new Stuff[2];
        stuff[0] = new Stuff();
        stuff[0].setKey(6);

        stuff[1] = new Stuff();
        stuff[1].setPhone("123456");

        User user = new User();
        user.setStuff(stuff);
        user.setNumbers(new int[] { 1, 2, 3});

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(user);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch(_docId, "numbers", roles -> roles.removeAt(1));
                session.advanced().patch(_docId, "stuff", roles -> roles.removeAt(0));
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);
                assertThat(loaded.getNumbers())
                        .hasSize(2);
                assertThat(loaded.getNumbers()[1])
                        .isEqualTo(3);

                assertThat(loaded.getStuff())
                        .hasSize(1);
                assertThat(loaded.getStuff()[0].getPhone())
                        .isEqualTo("123456");
            }
        }

    }

    @Test
    public void canIncrement() throws Exception {
        Stuff[] s = new Stuff[3];
        s[0] = new Stuff();
        s[0].setKey(6);

        User user = new User();
        user.setNumbers(new int[] { 66 });
        user.setStuff(s);

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(user);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().increment(_docId, "numbers[0]", 1);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);
                assertThat(loaded.getNumbers()[0])
                        .isEqualTo(67);

                session.advanced().increment(loaded, "stuff[0].key", -3);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loaded = session.load(User.class, _docId);
                assertThat(loaded.getStuff()[0].getKey())
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void shouldMergePatchCalls() throws Exception {
        Stuff[] stuff = new Stuff[3];
        stuff[0] = new Stuff();
        stuff[0].setKey(6);

        User user = new User();
        user.setStuff(stuff);
        user.setNumbers(new int[] { 66 });

        User user2 = new User();
        user2.setNumbers(new int[] { 1, 2,3 });
        user2.setStuff(stuff);

        String docId2 = "users/2-A";


        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(user);
                session.store(user2, docId2);
                session.saveChanges();
            }

            Date now = new Date();

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch(_docId, "numbers[0]", 31);
                assertThat(((InMemoryDocumentSessionOperations)session).getDeferredCommandsCount())
                        .isEqualTo(1);

                session.advanced().patch(_docId, "lastLogin", now);
                assertThat(((InMemoryDocumentSessionOperations)session).getDeferredCommandsCount())
                        .isEqualTo(1);

                session.advanced().patch(docId2, "numbers[0]", 123);
                assertThat(((InMemoryDocumentSessionOperations)session).getDeferredCommandsCount())
                        .isEqualTo(2);

                session.advanced().patch(docId2, "lastLogin", now);
                assertThat(((InMemoryDocumentSessionOperations)session).getDeferredCommandsCount())
                        .isEqualTo(2);


                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().increment(_docId, "numbers[0]", 1);
                assertThat(((InMemoryDocumentSessionOperations)session).getDeferredCommandsCount())
                        .isEqualTo(1);

                session.advanced().patch(_docId, "numbers", r -> r.add(77));
                assertThat(((InMemoryDocumentSessionOperations)session).getDeferredCommandsCount())
                        .isEqualTo(1);

                session.advanced().patch(_docId, "numbers", r-> r.add(88));
                assertThat(((InMemoryDocumentSessionOperations)session).getDeferredCommandsCount())
                        .isEqualTo(1);

                session.advanced().patch(_docId, "numbers", r -> r.removeAt(1));
                assertThat(((InMemoryDocumentSessionOperations)session).getDeferredCommandsCount())
                        .isEqualTo(1);

                session.saveChanges();
            }
        }
    }
}
