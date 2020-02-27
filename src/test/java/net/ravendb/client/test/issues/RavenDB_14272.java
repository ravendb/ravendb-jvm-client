package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.commands.StreamResult;
import net.ravendb.client.documents.queries.QueryData;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14272 extends RemoteTestBase {

    @Test
    public void select_Fields1() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            UserTalk userTalk = saveUserTalk(store);

            try (IDocumentSession session = store.openSession()) {
                List<TalkUserIds> result = session.query(UserTalk.class)
                        .selectFields(TalkUserIds.class)
                        .toList();

                assertThat(result)
                        .hasSize(1);
                assertThat(result.get(0).getUserDefs().size())
                        .isEqualTo(2);
                assertThat(userTalk.getUserDefs().keySet())
                        .contains(result.get(0).getUserDefs().keySet().toArray(new String[0]));
            }
        }
    }

    @Test
    public void select_Fields2() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            UserTalk userTalk = saveUserTalk(store);

            try (IDocumentSession session = store.openSession()) {
                List<TalkUserIds> result = session.query(UserTalk.class)
                        .selectFields(TalkUserIds.class, "userDefs")
                        .toList();

                assertThat(result)
                        .hasSize(1);
                assertThat(result.get(0).getUserDefs())
                        .hasSize(2);
                assertThat(userTalk.getUserDefs().keySet())
                        .contains(result.get(0).getUserDefs().keySet().toArray(new String[0]));
            }
        }
    }

    @Test
    public void select_Fields3() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            UserTalk userTalk = saveUserTalk(store);

            try (IDocumentSession session = store.openSession()) {
                QueryData queryData = new QueryData(new String[] { "userDefs" }, new String[] { "userDefs" });

                List<TalkUserIds> result = session.query(UserTalk.class)
                        .selectFields(TalkUserIds.class, queryData)
                        .toList();

                assertThat(result)
                        .hasSize(1);
                assertThat(result.get(0).getUserDefs())
                        .hasSize(2);
                assertThat(userTalk.getUserDefs().keySet())
                        .contains(result.get(0).getUserDefs().keySet().toArray(new String[0]));
            }
        }
    }

    @Test
    public void select_Fields4() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            UserTalk userTalk = saveUserTalk(store);

            try (IDocumentSession session = store.openSession()) {
                List<String> result = session.query(UserTalk.class)
                        .selectFields(String.class, "name")
                        .toList();

                assertThat(result)
                        .hasSize(1);
                assertThat(result.get(0))
                        .isEqualTo(userTalk.getName());

            }
        }
    }

    @Test
    public void streaming_query_projection() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            UserTalk userTalk = saveUserTalk(store);

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<TalkUserIds> query = session.query(UserTalk.class)
                        .selectFields(TalkUserIds.class);

                try (CloseableIterator<StreamResult<TalkUserIds>> stream = session.advanced().stream(query)) {
                    while (stream.hasNext()) {
                        TalkUserIds projection = stream.next().getDocument();

                        assertThat(projection)
                                .isNotNull();
                        assertThat(projection.getUserDefs())
                                .isNotNull()
                                .hasSize(2);

                        assertThat(userTalk.getUserDefs().keySet())
                                .contains(projection.getUserDefs().keySet().toArray(new String[0]));

                    }
                }
            }
        }
    }

    @Test
    public void streaming_document_query_projection() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            UserTalk userTalk = saveUserTalk(store);

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<TalkUserIds> query = session.advanced().documentQuery(UserTalk.class)
                        .selectFields(TalkUserIds.class, "userDefs");
                try (CloseableIterator<StreamResult<TalkUserIds>> stream = session.advanced().stream(query)) {
                    while (stream.hasNext()) {
                        TalkUserIds projection = stream.next().getDocument();
                        assertThat(projection)
                                .isNotNull();
                        assertThat(projection.getUserDefs())
                                .isNotNull()
                                .hasSize(2);
                        assertThat(userTalk.getUserDefs().keySet())
                                .contains(projection.getUserDefs().keySet().toArray(new String[0]));
                    }
                }
            }
        }
    }

    private UserTalk saveUserTalk(DocumentStore store) {
        UserTalk userTalk = new UserTalk();

        Map<String, TalkUserDef> userDefs = new HashMap<>();
        userDefs.put("test1", new TalkUserDef());
        userDefs.put("test2", new TalkUserDef());

        userTalk.setUserDefs(userDefs);
        userTalk.setName("Grisha");

        try (IDocumentSession session = store.openSession()) {
            session.store(userTalk);
            session.saveChanges();
        }

        return userTalk;
    }

    public static class UserTalk {
        private Map<String, TalkUserDef> userDefs;
        private String name;

        public Map<String, TalkUserDef> getUserDefs() {
            return userDefs;
        }

        public void setUserDefs(Map<String, TalkUserDef> userDefs) {
            this.userDefs = userDefs;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class TalkUserIds {
        private Map<String, TalkUserDef> userDefs;

        public Map<String, TalkUserDef> getUserDefs() {
            return userDefs;
        }

        public void setUserDefs(Map<String, TalkUserDef> userDefs) {
            this.userDefs = userDefs;
        }
    }

    public static class TalkUserDef {
        private String a;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }
    }
}
