package net.ravendb.client.executor;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.bulkInsert.BulkInsertOptions;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.http.*;
import net.ravendb.client.infrastructure.samples.User;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.test.client.bulkInsert.BulkInsertsTest;
import org.apache.http.Header;
import org.apache.http.HttpRequestInterceptor;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestExecutorCompressionTest extends RemoteTestBase {

    private static HttpRequestInterceptor interceptor;
    private static List<LoggedRequest> outputList;

    private HttpCompressionAlgorithm compressionAlgorithm;
    private boolean useHttpDecompression;
    private boolean useHttpCompression;

    static {
        interceptor = (request, context) -> {
            if (outputList != null) {
                Header acceptEncoding = request.getFirstHeader(Constants.Headers.ACCEPT_ENCODING);
                Header contentEncoding = request.getFirstHeader(Constants.Headers.CONTENT_ENCODING);

                outputList.add(new LoggedRequest(
                        request.getRequestLine().getUri(),
                        acceptEncoding != null ? acceptEncoding.getValue() : null,
                        contentEncoding != null ? contentEncoding.getValue() : null
                ));
            }
        };
    }

    @Test
    public void canUseGzipInRegularResponse() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInRegularResponse(HttpCompressionAlgorithm.Gzip);
        assertThat(loggedRequests)
                .hasSize(1);
        assertThat(loggedRequests.get(0).getAcceptEncoding())
                .isEqualTo(Constants.Headers.Encodings.GZIP);
    }

    @Test
    public void canUseZstdInRegularResponse() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInRegularResponse(HttpCompressionAlgorithm.Zstd);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        assertThat(loggedRequests.get(0).getAcceptEncoding())
                .isEqualTo(Constants.Headers.Encodings.ZSTD);
    }

    @Test
    public void canUseNoCompressionInRegularResponse() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInRegularResponse(null);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        assertThat(loggedRequests.get(0).getAcceptEncoding())
                .isNull();
    }

    @Test
    public void canUseGzipInRegularRequest() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInRegularRequest(HttpCompressionAlgorithm.Gzip);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        assertThat(loggedRequests.get(0).getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.GZIP);
    }

    @Test
    public void canUseZstdInRegularRequest() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInRegularRequest(HttpCompressionAlgorithm.Zstd);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        assertThat(loggedRequests.get(0).getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.ZSTD);
    }

    @Test
    public void canUseNoCompressionInRegularRequest() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInRegularRequest(null);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        assertThat(loggedRequests.get(0).getContentEncoding())
                .isNull();
    }

    @Test
    public void canUseGzipInBulkInsert() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInBulkInsert(HttpCompressionAlgorithm.Gzip);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest bulkInsert = loggedRequests.stream().filter(x -> x.getUrl().contains("bulk_insert")).findFirst().get();
        assertThat(bulkInsert.getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.GZIP);
    }

    @Test
    public void canUseZstdInBulkInsert() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInBulkInsert(HttpCompressionAlgorithm.Zstd);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest bulkInsert = loggedRequests.stream().filter(x -> x.getUrl().contains("bulk_insert")).findFirst().get();
        assertThat(bulkInsert.getContentEncoding())
                .isEqualTo(Constants.Headers.Encodings.ZSTD);
    }

    @Test
    public void canUseNoCompressionInBulkInsert() throws Exception {
        List<LoggedRequest> loggedRequests = canUseCompressionMethodInBulkInsert(null);
        assertThat(loggedRequests.size())
                .isGreaterThanOrEqualTo(1);
        LoggedRequest bulkInsert = loggedRequests.stream().filter(x -> x.getUrl().contains("bulk_insert")).findFirst().get();
        assertThat(bulkInsert.getContentEncoding())
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


    private List<LoggedRequest> canUseCompressionMethodInRegularResponse(HttpCompressionAlgorithm algorithm) throws Exception {
        this.useHttpDecompression = algorithm != null;
        this.useHttpCompression = false;
        this.compressionAlgorithm = algorithm;

        List<LoggedRequest> requests = new ArrayList<>();

        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setId("users/1");
                user.setName("Jill");

                session.store(user);

                try (CleanCloseable cleanCloseable = withRequestIntercept(requests)) {
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1");
                assertThat(user)
                        .isNotNull();
            }
        }

        return requests;
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

    private CleanCloseable withRequestIntercept(List<LoggedRequest> output) {
        outputList = output;

        return () -> outputList = null;
    }

    @Override
    protected void customizeStore(DocumentStore store) {
        //TODO: store.getConventions().setDisableTopologyUpdates(true); test fails when enabled
        store.getConventions().setUseHttpDecompression(useHttpDecompression);
        store.getConventions().setUseHttpCompression(useHttpCompression);
        if (compressionAlgorithm != null) {
            store.getConventions().setHttpCompressionAlgorithm(compressionAlgorithm);
        }
    }

    @BeforeAll
    public static void enableLogging() {
        RequestExecutor.configureHttpClient = (builder) -> {
            builder.addInterceptorLast(interceptor);
        };
    }

    @AfterAll
    public static void disableLogging() {
        RequestExecutor.configureHttpClient = null;
    }

    private static class LoggedRequest {
        private String url;
        private String acceptEncoding;
        private String contentEncoding;

        public LoggedRequest(String url, String acceptEncoding, String contentEncoding) {
            this.url = url;
            this.acceptEncoding = acceptEncoding;
            this.contentEncoding = contentEncoding;
        }

        @Override
        public String toString() {
            return "LoggedRequest{" +
                    "url='" + url + '\'' +
                    ", acceptEncoding='" + acceptEncoding + '\'' +
                    ", contentEncoding='" + contentEncoding + '\'' +
                    '}';
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
