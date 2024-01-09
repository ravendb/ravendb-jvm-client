package net.ravendb.client.executor;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.bulkInsert.BulkInsertOptions;
import net.ravendb.client.documents.operations.attachments.CloseableAttachmentResult;
import net.ravendb.client.documents.operations.attachments.PutAttachmentOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.http.*;
import net.ravendb.client.infrastructure.samples.User;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.test.client.bulkInsert.BulkInsertsTest;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.protocol.HttpClientContext;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestExecutorCompressionTest extends RemoteTestBase {

    private static final HttpRequestInterceptor requestInterceptor;
    private static final HttpResponseInterceptor responseInterceptor;
    private static List<LoggedRequest> requestLogList;
    private static List<LoggedResponse> responseLogList;

    private HttpCompressionAlgorithm compressionAlgorithm;
    private boolean useHttpDecompression;
    private boolean useHttpCompression;

    static {
        requestInterceptor = (request, context) -> {
            if (requestLogList != null) {

                if (request.getRequestLine().getUri().contains("/topology") || request.getRequestLine().getUri().contains("/node-info")) {
                    // ignore topology updates
                    return;
                }

                Header acceptEncoding = request.getFirstHeader(Constants.Headers.ACCEPT_ENCODING);
                Header contentEncoding = request.getFirstHeader(Constants.Headers.CONTENT_ENCODING);

                requestLogList.add(new LoggedRequest(
                        request.getRequestLine().getMethod(),
                        request.getRequestLine().getUri(),
                        acceptEncoding != null ? acceptEncoding.getValue() : null,
                        contentEncoding != null ? contentEncoding.getValue() : null
                ));
            }
        };

        responseInterceptor = (response, context) -> {
            if (responseLogList != null) {
                Header contentEncoding = response.getFirstHeader(Constants.Headers.CONTENT_ENCODING);
                HttpClientContext httpClientContext = (HttpClientContext) context;
                HttpRequest request = httpClientContext.getRequest();

                if (request.getRequestLine().getUri().contains("/topology") || request.getRequestLine().getUri().contains("/node-info")) {
                    // ignore topology updates
                    return;
                }

                responseLogList.add(new LoggedResponse(request.getRequestLine().getMethod(),
                        request.getRequestLine().getUri(),
                        contentEncoding != null ? contentEncoding.getValue() : null));
            }
        };
    }

    @Test
    public void canUseGzipInRegularResponse() throws Exception {
        List<LoggedRequest> loggedRequests = new ArrayList<>();
        List<LoggedResponse> loggedResponses = new ArrayList<>();

        canUseCompressionMethodInRegularResponse(HttpCompressionAlgorithm.Gzip, loggedRequests, loggedResponses);
        assertThat(loggedRequests)
                .hasSize(1);
        assertThat(loggedRequests.get(0).getAcceptEncoding())
                .isEqualTo(Constants.Headers.Encodings.GZIP);

        assertThat(loggedResponses)
                .hasSize(1);
        assertThat(loggedResponses.get(0).getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.GZIP);
    }

    @Test
    public void canUseZstdInRegularResponse() throws Exception {
        List<LoggedRequest> loggedRequests = new ArrayList<>();
        List<LoggedResponse> loggedResponses = new ArrayList<>();

        canUseCompressionMethodInRegularResponse(HttpCompressionAlgorithm.Zstd, loggedRequests, loggedResponses);
        assertThat(loggedRequests.size())
                .isEqualTo(1);
        assertThat(loggedRequests.get(0).getAcceptEncoding())
                .isEqualTo(Constants.Headers.Encodings.ZSTD);

        assertThat(loggedResponses)
                .hasSize(1);
        assertThat(loggedResponses.get(0).getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.ZSTD);
    }

    @Test
    public void canUseNoCompressionInRegularResponse() throws Exception {
        List<LoggedRequest> loggedRequests = new ArrayList<>();
        List<LoggedResponse> loggedResponses = new ArrayList<>();

        canUseCompressionMethodInRegularResponse(null, loggedRequests, loggedResponses);
        assertThat(loggedRequests.size())
                .isEqualTo(1);
        assertThat(loggedRequests.get(0).getAcceptEncoding())
                .isNull();

        assertThat(loggedResponses)
                .hasSize(1);
        assertThat(loggedResponses.get(0).getContentEncoding())
                .isNull();
    }

    @Test
    public void canUseGzipInRegularRequest() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInRegularRequest(HttpCompressionAlgorithm.Gzip);
        assertThat(loggedRequests.size())
                .isEqualTo(1);
        assertThat(loggedRequests.get(0).getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.GZIP);
    }

    @Test
    public void canUseZstdInRegularRequest() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInRegularRequest(HttpCompressionAlgorithm.Zstd);
        assertThat(loggedRequests.size())
                .isEqualTo(1);
        assertThat(loggedRequests.get(0).getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.ZSTD);
    }

    @Test
    public void canUseNoCompressionInRegularRequest() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInRegularRequest(null);
        assertThat(loggedRequests.size())
                .isEqualTo(1);
        assertThat(loggedRequests.get(0).getContentEncoding())
                .isNull();
    }

    @Test
    public void canUseGzipInBulkInsert() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInBulkInsert(HttpCompressionAlgorithm.Gzip);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest bulkInsert = loggedRequests.stream().filter(x -> x.getUrl().contains("bulk_insert")).findFirst().orElse(null);
        assertThat(bulkInsert.getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.GZIP);
    }

    @Test
    public void canUseZstdInBulkInsert() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInBulkInsert(HttpCompressionAlgorithm.Zstd);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest bulkInsert = loggedRequests.stream().filter(x -> x.getUrl().contains("bulk_insert")).findFirst().orElse(null);
        assertThat(bulkInsert.getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.ZSTD);
    }

    @Test
    public void canUseNoCompressionInBulkInsert() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInBulkInsert(null);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest bulkInsert = loggedRequests.stream().filter(x -> x.getUrl().contains("bulk_insert")).findFirst().orElse(null);
        assertThat(bulkInsert.getContentEncoding())
                .isNull();
    }

    @Test
    public void canUseZstdWithAttachmentsViaSession() throws Exception {
        List<LoggedRequest> loggedRequests = new ArrayList<>();
        List<LoggedResponse> loggedResponses = new ArrayList<>();
        putAttachmentViaSession(HttpCompressionAlgorithm.Zstd, loggedRequests, loggedResponses);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest loggedRequest = loggedRequests.stream().filter(x -> "POST".equals(x.method)).findFirst().orElse(null);
        assertThat(loggedRequest)
                .isNotNull();

        // we don't validate content entity here as it is multipart request and only json part is encoded.
        // assertThat(loggedRequest.getContentEncoding())
        //        .isEqualTo(Constants.Headers.Encodings.ZSTD);

        assertThat(loggedRequest.getAcceptEncoding())
                .isEqualTo(Constants.Headers.Encodings.ZSTD);

        assertThat(loggedResponses.size())
                .isGreaterThanOrEqualTo(1);

        LoggedResponse loggedResponse = loggedResponses.stream().filter(x -> "POST".equals(x.method)).findFirst().orElse(null);
        assertThat(loggedResponse.getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.ZSTD);
    }

    @Test
    public void canUseGzipWithAttachmentsViaSession() throws Exception {
        List<LoggedRequest> loggedRequests = new ArrayList<>();
        List<LoggedResponse> loggedResponses = new ArrayList<>();
        putAttachmentViaSession(HttpCompressionAlgorithm.Gzip, loggedRequests, loggedResponses);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest loggedRequest = loggedRequests.stream().filter(x -> "POST".equals(x.method)).findFirst().orElse(null);
        assertThat(loggedRequest)
                .isNotNull();

        // we don't validate content entity here as it is multipart request and only json part is encoded.
        // assertThat(loggedRequest.getContentEncoding())
        //        .isEqualTo(Constants.Headers.Encodings.GZIP);

        assertThat(loggedRequest.getAcceptEncoding())
                .isEqualTo(Constants.Headers.Encodings.GZIP);

        assertThat(loggedResponses.size())
                .isGreaterThanOrEqualTo(1);

        LoggedResponse loggedResponse = loggedResponses.stream().filter(x -> "POST".equals(x.method)).findFirst().orElse(null);
        assertThat(loggedResponse.getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.GZIP);
    }

    @Test
    public void canUseNoCompressionWithAttachmentsViaSession() throws Exception {
        List<LoggedRequest> loggedRequests = new ArrayList<>();
        List<LoggedResponse> loggedResponses = new ArrayList<>();
        putAttachmentViaSession(null, loggedRequests, loggedResponses);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest loggedRequest = loggedRequests.stream().filter(x -> "POST".equals(x.method)).findFirst().orElse(null);
        assertThat(loggedRequest)
                .isNotNull();

        // we don't validate content entity here as it is multipart request and only json part is encoded.
        // assertThat(loggedRequest.getContentEncoding())
        //        .isNull();

        assertThat(loggedRequest.getAcceptEncoding())
                .isNull();

        assertThat(loggedResponses.size())
                .isGreaterThanOrEqualTo(1);

        LoggedResponse loggedResponse = loggedResponses.stream().filter(x -> "POST".equals(x.method)).findFirst().orElse(null);
        assertThat(loggedResponse.getContentEncoding())
                .isNull();
    }

    @Test
    public void canUseZstdWithAttachmentsViaOperation() throws Exception {
        List<LoggedRequest> loggedRequests = new ArrayList<>();
        List<LoggedResponse> loggedResponses = new ArrayList<>();
        putAttachmentViaOperation(HttpCompressionAlgorithm.Zstd, loggedRequests, loggedResponses);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest loggedRequest = loggedRequests.stream().filter(x -> "PUT".equals(x.method)).findFirst().orElse(null);
        assertThat(loggedRequest)
                .isNotNull();
        assertThat(loggedRequest.getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.ZSTD);

        assertThat(loggedResponses)
                .hasSize(1);

        // RavenDB-21919 - we don't compress attachments
        assertThat(loggedResponses.get(0).getContentEncoding())
                .isNull();
    }

    @Test
    public void canUseGzipWithAttachmentsViaOperation() throws Exception {
        List<LoggedRequest> loggedRequests = new ArrayList<>();
        List<LoggedResponse> loggedResponses = new ArrayList<>();
        putAttachmentViaOperation(HttpCompressionAlgorithm.Gzip, loggedRequests, loggedResponses);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest loggedRequest = loggedRequests.stream().filter(x -> "PUT".equals(x.method)).findFirst().orElse(null);
        assertThat(loggedRequest)
                .isNotNull();
        assertThat(loggedRequest.getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.GZIP);

        assertThat(loggedResponses)
                .hasSize(1);

        // RavenDB-21919 - we don't compress attachments
        assertThat(loggedResponses.get(0).getContentEncoding())
                .isNull();
    }

    @Test
    public void canUseNoCompressionWithAttachmentsViaOperation() throws Exception {
        List<LoggedRequest> loggedRequests = new ArrayList<>();
        List<LoggedResponse> loggedResponses = new ArrayList<>();
        putAttachmentViaOperation(null, loggedRequests, loggedResponses);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest loggedRequest = loggedRequests.stream().filter(x -> "PUT".equals(x.method)).findFirst().orElse(null);
        assertThat(loggedRequest)
                .isNotNull();
        assertThat(loggedRequest.getContentEncoding())
                .isNull();

        assertThat(loggedResponses)
                .hasSize(1);
        assertThat(loggedResponses.get(0).getContentEncoding())
                .isNull();
    }

    private List<LoggedRequest> canUseCompressionMethodInRegularRequest(HttpCompressionAlgorithm algorithm) throws Exception {
        this.useHttpDecompression = false;
        this.useHttpCompression = algorithm != null;
        this.compressionAlgorithm = algorithm;

        List<LoggedRequest> requests = new ArrayList<>();

        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setId("users-request/1");
                user.setName("Jill");

                session.store(user);
                try (CleanCloseable cleanCloseable = withRequestIntercept(requests)) {
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users-request/1");
                assertThat(user)
                        .isNotNull();
            }
        }

        return requests;
    }


    private void canUseCompressionMethodInRegularResponse(HttpCompressionAlgorithm algorithm, List<LoggedRequest> loggedRequests, List<LoggedResponse> loggedResponses) throws Exception {
        this.useHttpDecompression = algorithm != null;
        this.useHttpCompression = false;
        this.compressionAlgorithm = algorithm;

        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setId("users/1");
                user.setName("Jill");

                session.store(user);

                try (CleanCloseable cleanCloseable = withRequestIntercept(loggedRequests)) {
                    try (CleanCloseable closeable = withResponseInterceptor(loggedResponses)) {
                        session.saveChanges();
                    }
                }
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1");
                assertThat(user)
                        .isNotNull();
            }
        }
    }

    private List<LoggedRequest> canUseCompressionMethodInBulkInsert(HttpCompressionAlgorithm algorithm) throws Exception {
        this.useHttpDecompression = false;
        this.useHttpCompression = algorithm != null;
        this.compressionAlgorithm = algorithm;

        List<LoggedRequest> requests = new ArrayList<>();

        BulkInsertsTest.FooBar fooBar1 = new BulkInsertsTest.FooBar();
        fooBar1.setName("John Doe");

        BulkInsertsTest.FooBar fooBar2 = new BulkInsertsTest.FooBar();
        fooBar2.setName("Jane Doe");

        BulkInsertsTest.FooBar fooBar3 = new BulkInsertsTest.FooBar();
        fooBar3.setName("Mega John");

        BulkInsertsTest.FooBar fooBar4 = new BulkInsertsTest.FooBar();
        fooBar4.setName("Mega Jane");

        try (IDocumentStore store = getDocumentStore()) {
            BulkInsertOptions bulkInsertOptions = new BulkInsertOptions();
            bulkInsertOptions.setUseCompression(algorithm != null);

            try (CleanCloseable cleanCloseable = withRequestIntercept(requests)) {
                try (BulkInsertOperation bulkInsert = store.bulkInsert(bulkInsertOptions)) {
                    bulkInsert.store(fooBar1);
                    bulkInsert.store(fooBar2);
                    bulkInsert.store(fooBar3);
                    bulkInsert.store(fooBar4);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                BulkInsertsTest.FooBar doc1 = session.load(BulkInsertsTest.FooBar.class, "FooBars/1-A");
                BulkInsertsTest.FooBar doc2 = session.load(BulkInsertsTest.FooBar.class, "FooBars/2-A");
                BulkInsertsTest.FooBar doc3 = session.load(BulkInsertsTest.FooBar.class, "FooBars/3-A");
                BulkInsertsTest.FooBar doc4 = session.load(BulkInsertsTest.FooBar.class, "FooBars/4-A");

                assertThat(doc1)
                        .isNotNull();
                assertThat(doc2)
                        .isNotNull();
                assertThat(doc3)
                        .isNotNull();
                assertThat(doc4)
                        .isNotNull();
            }
        }

        return requests;
    }

    private void putAttachmentViaSession(HttpCompressionAlgorithm algorithm, List<LoggedRequest> loggedRequests, List<LoggedResponse> loggedResponses) throws Exception {
        this.useHttpDecompression = algorithm != null;
        this.useHttpCompression = algorithm != null;
        this.compressionAlgorithm = algorithm;

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                ByteArrayInputStream profileStream = new ByteArrayInputStream(new byte[]{1, 2, 3});

                net.ravendb.client.infrastructure.entities.User user = new net.ravendb.client.infrastructure.entities.User();
                user.setName("Marcin");

                session.store(user, "users/1");

                session.advanced().attachments().store("users/1", "profile.png", profileStream, "image/png");

                try (CleanCloseable cleanCloseable = withRequestIntercept(loggedRequests)) {
                    try (CleanCloseable closeable = withResponseInterceptor(loggedResponses)) {
                        session.saveChanges();
                    }
                }
            }

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable cleanCloseable = withRequestIntercept(loggedRequests);
                     CleanCloseable closeable = withResponseInterceptor(loggedResponses);
                     CloseableAttachmentResult closeableAttachmentResult = session.advanced().attachments().get("users/1", "profile.png")) {
                    byte[] byteArray = IOUtils.toByteArray(closeableAttachmentResult.getData());
                    assertThat(byteArray)
                            .hasSize(3);
                    assertThat(byteArray[0])
                            .isEqualTo((byte) 1);
                    assertThat(byteArray[1])
                            .isEqualTo((byte) 2);
                    assertThat(byteArray[2])
                            .isEqualTo((byte) 3);
                }
            }
        }
    }

    private void putAttachmentViaOperation(HttpCompressionAlgorithm algorithm, List<LoggedRequest> requests, List<LoggedResponse> responses) throws Exception {
        this.useHttpDecompression = algorithm != null;
        this.useHttpCompression = algorithm != null;
        this.compressionAlgorithm = algorithm;

        try (IDocumentStore store = getDocumentStore()) {

            ByteArrayInputStream profileStream = new ByteArrayInputStream(new byte[]{1, 2, 3});

            try (IDocumentSession session = store.openSession()) {
                net.ravendb.client.infrastructure.entities.User user = new net.ravendb.client.infrastructure.entities.User();
                user.setName("Marcin");

                session.store(user, "users/1");
                session.saveChanges();
            }

            try (CleanCloseable cleanCloseable = withRequestIntercept(requests)) {
                store.operations().send(new PutAttachmentOperation("users/1", "profile.png", profileStream, "image/png"));
            }

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable cleanCloseable = withRequestIntercept(requests);
                     CleanCloseable closeable = withResponseInterceptor(responses);
                     CloseableAttachmentResult closeableAttachmentResult = session.advanced().attachments().get("users/1", "profile.png")) {
                    byte[] byteArray = IOUtils.toByteArray(closeableAttachmentResult.getData());
                    assertThat(byteArray)
                            .hasSize(3);
                    assertThat(byteArray[0])
                            .isEqualTo((byte) 1);
                    assertThat(byteArray[1])
                            .isEqualTo((byte) 2);
                    assertThat(byteArray[2])
                            .isEqualTo((byte) 3);
                }
            }
        }
    }

    private CleanCloseable withRequestIntercept(List<LoggedRequest> output) {
        requestLogList = output;

        return () -> requestLogList = null;
    }

    private CleanCloseable withResponseInterceptor(List<LoggedResponse> output) {
        responseLogList = output;

        return () -> responseLogList = null;
    }

    @Override
    protected void customizeStore(DocumentStore store) {
        store.getConventions().setUseHttpDecompression(useHttpDecompression);
        store.getConventions().setUseHttpCompression(useHttpCompression);
        if (compressionAlgorithm != null) {
            store.getConventions().setHttpCompressionAlgorithm(compressionAlgorithm);
        }
    }

    @BeforeAll
    public static void enableLogging() {
        RequestExecutor.configureHttpClient = (builder) -> {
            builder.addInterceptorLast(requestInterceptor);
            builder.addInterceptorFirst(responseInterceptor);
        };
    }

    @AfterAll
    public static void disableLogging() {
        RequestExecutor.configureHttpClient = null;
    }

    private static class LoggedResponse {
        private final String method;
        private final String url;
        private final String contentEncoding;

        public LoggedResponse(String method, String url, String contentEncoding) {
            this.method = method;
            this.url = url;
            this.contentEncoding = contentEncoding;
        }

        @Override
        public String toString() {
            return "LoggedResponse{" +
                    "method='" + method + '\'' +
                    ", url='" + url + '\'' +
                    ", contentEncoding='" + contentEncoding + '\'' +
                    '}';
        }

        public String getMethod() {
            return method;
        }

        public String getUrl() {
            return url;
        }

        public String getContentEncoding() {
            return contentEncoding;
        }
    }

    private static class LoggedRequest {
        private final String method;
        private final String url;
        private final String acceptEncoding;
        private final String contentEncoding;

        public LoggedRequest(String method, String url, String acceptEncoding, String contentEncoding) {
            this.method = method;
            this.url = url;
            this.acceptEncoding = acceptEncoding;
            this.contentEncoding = contentEncoding;
        }

        @Override
        public String toString() {
            return "LoggedRequest{" +
                    "method='" + method + '\'' +
                    ", url='" + url + '\'' +
                    ", acceptEncoding='" + acceptEncoding + '\'' +
                    ", contentEncoding='" + contentEncoding + '\'' +
                    '}';
        }

        public String getMethod() {
            return method;
        }

        public String getUrl() {
            return url;
        }

        public String getAcceptEncoding() {
            return acceptEncoding;
        }

        public String getContentEncoding() {
            return contentEncoding;
        }
    }


}
