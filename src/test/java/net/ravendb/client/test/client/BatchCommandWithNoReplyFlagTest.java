package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.commands.batches.JsonPatchCommandData;
import net.ravendb.client.documents.commands.batches.JsonPatchDocument;
import net.ravendb.client.documents.commands.batches.PatchCommandData;
import net.ravendb.client.documents.commands.batches.SingleNodeBatchCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.PatchRequest;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchCommandWithNoReplyFlagTest extends RemoteTestBase {

    private static final String USER_ID = "users/1";
    private static final String USER_VALUE = "SomeValue";

    @Test
    public void batchWithNoReply_whenPatch_shouldNotFail() throws Exception {
        PatchRequest patchRequest = new PatchRequest();
        patchRequest.setScript("this.name = args.val_0;");
        patchRequest.setValues(Collections.singletonMap("val_0", USER_VALUE));

        assertBatchWithNoReplyApplies(new PatchCommandData(USER_ID, null, patchRequest));
    }

    @Test
    public void batchWithNoReply_whenJsonPatch_shouldNotFail() throws Exception {
        assertBatchWithNoReplyApplies(new JsonPatchCommandData(USER_ID, new JsonPatchDocument().add("/name", USER_VALUE)));
    }

    private void assertBatchWithNoReplyApplies(ICommandData command) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new TestObj(), USER_ID);
                session.saveChanges();
            }

            try (NoReplySingleNodeBatchCommand batch = new NoReplySingleNodeBatchCommand(store.getConventions(),
                    Collections.singletonList(command))) {
                store.getRequestExecutor().execute(batch);
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(TestObj.class, USER_ID).getName()).isEqualTo(USER_VALUE);
            }
        }
    }

    private static class NoReplySingleNodeBatchCommand extends SingleNodeBatchCommand {
        NoReplySingleNodeBatchCommand(DocumentConventions conventions, List<ICommandData> commands) {
            super(conventions, commands);
        }

        @Override
        protected void appendOptions(StringBuilder sb) {
            super.appendOptions(sb);
            sb.append(sb.charAt(sb.length() - 1) == '?' ? "" : "&").append("noreply=true");
        }
    }

    public static class TestObj {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
