package net.ravendb.client.test.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.commands.PutDocumentCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.EntityToJson;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.RequestAbortedException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class RequestExecutorTest extends ClusterTestBase {

    @Test
    public void onBeforeAfterAndFailRequest1() throws Exception {
        onBeforeAfterAndFailRequestInternal(0, 1, new String[] { "OnBeforeRequest", "OnAfterRequests" });
    }

    @Test
    public void onBeforeAfterAndFailRequest2() throws Exception {
        onBeforeAfterAndFailRequestInternal(1, 2, new String[] { "OnBeforeRequest", "OnFailedRequest", "OnBeforeRequest", "OnAfterRequests" });
    }

    @Test
    public void onBeforeAfterAndFailRequest3() throws Exception {
        onBeforeAfterAndFailRequestInternal(2, 2, new String[] { "OnBeforeRequest", "OnFailedRequest", "OnBeforeRequest" });
    }

    private void onBeforeAfterAndFailRequestInternal(int failCount, int clusterSize, String[] expected) throws Exception {
        List<String> actual = new ArrayList<>();
        List<String> sessionActual = new ArrayList<>();

        String urlRegex = ".*/databases/[^/]+/docs.*";

        try (ClusterController cluster = createRaftCluster(clusterSize)) {
            ClusterNode leader = cluster.getInitialLeader();

            String databaseName = getDatabaseName();

            cluster.createDatabase(new DatabaseRecord(databaseName), clusterSize, cluster.getInitialLeader().getUrl());

            try (DocumentStore store = new DocumentStore(leader.getUrl(), databaseName)) {
                store.addOnBeforeRequestListener((sender, event) -> {
                    if (!event.getUrl().matches(urlRegex)) {
                        return;
                    }

                    sessionActual.add("OnBeforeRequest");
                });

                store.addOnSucceedRequestListener((sender, event) -> {
                    if (!event.getUrl().matches(urlRegex)) {
                        return;
                    }

                    sessionActual.add("OnAfterRequests");
                });

                store.addOnFailedRequestListener((sender, event) -> {
                    if (!event.getUrl().matches(urlRegex)) {
                        return;
                    }

                    sessionActual.add("OnFailedRequest");
                });

                store.initialize();

                RequestExecutor requestExecutor = store.getRequestExecutor();

                requestExecutor.addOnBeforeRequestListener((sender, event) -> {
                    if (!event.getUrl().matches(urlRegex)) {
                        return;
                    }

                    actual.add("OnBeforeRequest");
                });

                requestExecutor.addOnSucceedRequestListener((sender, event) -> {
                    if (!event.getUrl().matches(urlRegex)) {
                        return;
                    }

                    actual.add("OnAfterRequests");
                });

                requestExecutor.addOnFailedRequestListener((sender, event) -> {
                    if (!event.getUrl().matches(urlRegex)) {
                        return;
                    }

                    actual.add("OnFailedRequest");
                });

                ObjectNode documentJson = EntityToJson.convertEntityToJson(new User(), store.getConventions(), null);
                FirstFailCommand command = new FirstFailCommand(store.getConventions(), "User/1", null, documentJson, failCount);
                try {
                    requestExecutor.execute(command);
                } catch (Exception e) {
                    // ignored
                }

                assertThat(actual)
                        .containsSequence(expected);
                assertThat(sessionActual)
                        .containsSequence(expected);

            }
        }
    }

    public static class FirstFailCommand extends PutDocumentCommand {
        private int _timeToFail;

        public FirstFailCommand(DocumentConventions conventions, String id, String changeVector, ObjectNode document, int timeToFail) {
            super(conventions, id, changeVector, document);

            _timeToFail = timeToFail;
        }

        @Override
        public CloseableHttpResponse send(CloseableHttpClient client, HttpUriRequestBase request) throws IOException {
            _timeToFail--;
            if (_timeToFail < 0) {
                return super.send(client, request);
            }

            throw new RequestAbortedException("Just testing");
        }
    }
}
