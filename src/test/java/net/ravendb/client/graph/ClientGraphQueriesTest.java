package net.ravendb.client.graph;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOn60Server;
import net.ravendb.client.primitives.NetISO8601Utils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
@DisabledOn60Server
public class ClientGraphQueriesTest extends RemoteTestBase {

    @Test
    public void canGraphQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Bar bar = new Bar();
                bar.setName("Barvazon");

                String barId = "Bars/1";

                session.store(bar, barId);

                Foo foo = new Foo();
                foo.setName("Foozy");
                foo.setBars(Collections.singletonList(barId));

                session.store(foo);

                session.saveChanges();

                FooBar res = session.advanced()
                        .graphQuery(FooBar.class, "match (Foos as foo)-[bars as _]->(Bars as bar)")
                        .with("foo", session.query(Foo.class))
                        .single();

                assertThat(res.getFoo().getName())
                        .isEqualTo("Foozy");

                assertThat(res.getBar().getName())
                        .isEqualTo("Barvazon");
            }
        }
    }

    @Test
    public void canAggregateQueryParametersProperly() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Bar bar = new Bar();
                bar.setName("Barvazon");
                bar.setAge(19);

                String barId = "Bars/1";

                session.store(bar, barId);

                Foo foo = new Foo();
                foo.setName("Foozy");
                foo.setBars(Collections.singletonList(barId));

                session.store(foo);

                session.saveChanges();

                List<List<String>> namesList = Arrays.asList(
                        Arrays.asList("Fi", "Fah", "Foozy"),
                        Arrays.asList("Fi", "Foozy", "Fah"),
                        Arrays.asList("Foozy", "Fi", "Fah", "Foozy"),
                        Arrays.asList("Fi", "Foozy", "Fah", "Fah", "Foozy")
                );

                for (List<String> names : namesList) {
                    List<FooBar> res = session.advanced().graphQuery(FooBar.class, "match (Foos as foo)-[bars as _]->(Bars as bar)")
                            .with("foo", builder -> builder.query(Foo.class).whereIn("name", names))
                            .with("bar", session.query(Bar.class).whereGreaterThanOrEqual("age", 18))
                            .waitForNonStaleResults()
                            .toList();

                    assertThat(res)
                            .hasSize(1);

                    assertThat(res.get(0).getFoo().getName())
                            .isEqualTo("Foozy");
                    assertThat(res.get(0).getBar().getName())
                            .isEqualTo("Barvazon");
                }
            }
        }
    }

    @Test
    public void waitForNonStaleResultsOnGraphQueriesWithClauseShouldWork() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            List<String> names = Arrays.asList("Fi", "Fah", "Foozy");

            try (IDocumentSession session = store.openSession()) {
                IndexQuery query = session
                        .advanced()
                        .graphQuery(FooBar.class, "match (Foos as foo)-[bars as _]->(Bars as bar)")
                        .with("foo", builder -> builder.query(Foo.class).whereIn("name", names).waitForNonStaleResults(Duration.ofMinutes(3)))
                        .with("bar", session
                                .query(Bar.class)
                                .waitForNonStaleResults(Duration.ofMinutes(5))
                                .whereGreaterThanOrEqual("age", 18))
                        .waitForNonStaleResults()
                        .getIndexQuery();

                assertThat(query.isWaitForNonStaleResults())
                        .isTrue();
                assertThat(query.getWaitForNonStaleResultsTimeout())
                        .isEqualTo(Duration.ofMinutes(5));
            }
        }
    }

    @Test
    public void canUseWithEdges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Date now = new Date();

                Friend friend1 = new Friend();
                friend1.setName("F1");
                friend1.setAge(21);
                friend1.setFriends(new FriendDescriptor[] {
                        new FriendDescriptor(DateUtils.addDays(now, -1024), "Friend/2"),
                        new FriendDescriptor(DateUtils.addDays(now, -678), "Friend/3"),
                        new FriendDescriptor(DateUtils.addDays(now, -345), "Friend/4")
                });

                session.store(friend1, "Friend/1");

                Friend friend2 = new Friend();
                friend2.setName("F2");
                friend2.setAge(19);
                friend2.setFriends(new FriendDescriptor[] {
                        new FriendDescriptor(DateUtils.addDays(now, -1024), "Friend/1"),
                        new FriendDescriptor(DateUtils.addDays(now, -304), "Friend/4")
                });

                session.store(friend2, "Friend/2");

                Friend friend3 = new Friend();
                friend3.setName("F3");
                friend3.setAge(41);
                friend3.setFriends(new FriendDescriptor[] {
                        new FriendDescriptor(DateUtils.addDays(now, -678), "Friend/1")
                });

                session.store(friend3, "Friend/3");

                Friend friend4 = new Friend();
                friend4.setName("F4");
                friend4.setAge(32);
                friend4.setFriends(new FriendDescriptor[] {
                        new FriendDescriptor(DateUtils.addDays(now, -304), "Friend/2"),
                        new FriendDescriptor(DateUtils.addDays(now, -345), "Friend/1")
                });

                session.store(friend4, "Friend/4");

                Date from = DateUtils.addDays(now, -345);

                session.saveChanges();

                List<FriendsTuple> tupleResult = session.advanced().graphQuery(FriendsTuple.class, "match (f1)-[l1]->(f2)")
                        .with("f1", session.query(Friend.class))
                        .with("f2", session.query(Friend.class))
                        .withEdges("l1", "friends", "where friendsSince >= '" + NetISO8601Utils.format(from, true) + "' select friendId")
                        .waitForNonStaleResults()
                        .toList();

                List<Friend> res = tupleResult
                        .stream()
                        .sorted(Comparator.<FriendsTuple> comparingInt(x -> x.getF1().getAge()).reversed())
                        .map(FriendsTuple::getF1)
                        .collect(Collectors.toList());

                assertThat(res)
                        .hasSize(4);

                assertThat(res.get(0).getName())
                        .isEqualTo("F4");
                assertThat(res.get(1).getName())
                        .isEqualTo("F4");
                assertThat(res.get(2).getName())
                        .isEqualTo("F1");
                assertThat(res.get(3).getName())
                        .isEqualTo("F2");
            }
        }
    }

    public static class FriendsTuple {
        private Friend f1;
        private FriendDescriptor l1;
        private Friend f2;

        public Friend getF1() {
            return f1;
        }

        public void setF1(Friend f1) {
            this.f1 = f1;
        }

        public FriendDescriptor getL1() {
            return l1;
        }

        public void setL1(FriendDescriptor l1) {
            this.l1 = l1;
        }

        public Friend getF2() {
            return f2;
        }

        public void setF2(Friend f2) {
            this.f2 = f2;
        }
    }

    public static class Friend {
        private String name;
        private int age;
        private FriendDescriptor[] friends;

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

        public FriendDescriptor[] getFriends() {
            return friends;
        }

        public void setFriends(FriendDescriptor[] friends) {
            this.friends = friends;
        }
    }


    public static class FooBar {
        private Foo foo;
        private Bar bar;

        public Foo getFoo() {
            return foo;
        }

        public void setFoo(Foo foo) {
            this.foo = foo;
        }

        public Bar getBar() {
            return bar;
        }

        public void setBar(Bar bar) {
            this.bar = bar;
        }
    }

    public static class Foo {
        private String name;
        private List<String> bars;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getBars() {
            return bars;
        }

        public void setBars(List<String> bars) {
            this.bars = bars;
        }
    }

    public static class Bar {
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


    public static class FriendDescriptor {
        private Date friendsSince;
        private String friendId;

        public FriendDescriptor() {
        }

        public FriendDescriptor(Date friendsSince, String friendId) {
            this.friendsSince = friendsSince;
            this.friendId = friendId;
        }

        public Date getFriendsSince() {
            return friendsSince;
        }

        public void setFriendsSince(Date friendsSince) {
            this.friendsSince = friendsSince;
        }

        public String getFriendId() {
            return friendId;
        }

        public void setFriendId(String friendId) {
            this.friendId = friendId;
        }
    }

}
