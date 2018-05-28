package net.ravendb.client;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.test.driver.RavenServerLocator;
import net.ravendb.client.test.driver.RavenTestDriver;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;

import java.net.MalformedURLException;
import java.net.URL;

@SuppressWarnings("SameParameterValue")
public class RemoteTestBase extends RavenTestDriver {

    private static class TestServiceLocator extends RavenServerLocator {

        @Override
        public String[] getCommandArguments() {
            return new String[] { "--ServerUrl=http://127.0.0.1:0", "--ServerUrl.Tcp=tcp://127.0.0.1:38881" };
        }
    }

    private static class TestSecuredServiceLocator extends RavenServerLocator {

        public static final String ENV_CERTIFICATE_PATH = "RAVENDB_JAVA_TEST_CERTIFICATE_PATH";

        public static final String ENV_HTTPS_SERVER_URL = "RAVENDB_JAVA_TEST_HTTPS_SERVER_URL";

        @Override
        public String[] getCommandArguments() {
            String httpsServerUrl = getHttpsServerUrl();

            try {
                URL url = new URL(httpsServerUrl);
                String host = url.getHost();
                String tcpServerUrl = "tcp://" + host + ":38882";

                return new String[]{
                        "--Security.Certificate.Path=" + getServerCertificatePath(),
                        "--ServerUrl=" + httpsServerUrl,
                        "--ServerUrl.Tcp=" + tcpServerUrl
                };
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        private String getHttpsServerUrl() {
            String httpsServerUrl = System.getenv(ENV_HTTPS_SERVER_URL);
            if (StringUtils.isBlank(httpsServerUrl)) {
                throw new IllegalStateException("Unable to find RavenDB https server url. " +
                        "Please make sure " + ENV_HTTPS_SERVER_URL + " environment variable is set and is valid " +
                        "(current value = " + httpsServerUrl + ")");
            }

            return httpsServerUrl;
        }

        @Override
        public String getServerCertificatePath() {
            String certificatePath = System.getenv(ENV_CERTIFICATE_PATH);
            if (StringUtils.isBlank(certificatePath)) {
                throw new IllegalStateException("Unable to find RavenDB server certificate path. " +
                        "Please make sure " + ENV_CERTIFICATE_PATH + " environment variable is set and is valid " +
                        "(current value = " + certificatePath + ")");
            }

            return certificatePath;
        }
    }

    public RemoteTestBase() {
        super(new TestServiceLocator(), new TestSecuredServiceLocator());
    }


    public CleanCloseable withFiddler() {
        RequestExecutor.requestPostProcessor = request -> {
            HttpHost proxy = new HttpHost("127.0.0.1", 8888, "http");
            RequestConfig requestConfig = request.getConfig();
            if (requestConfig == null) {
                requestConfig = RequestConfig.DEFAULT;
            }
            requestConfig = RequestConfig.copy(requestConfig).setProxy(proxy).build();
            request.setConfig(requestConfig);
        };

        return () -> RequestExecutor.requestPostProcessor = null;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected ConfigureRevisionsOperation.ConfigureRevisionsOperationResult setupRevisions(IDocumentStore store, boolean purgeOnDelete, long minimumRevisionsToKeep) {
        RevisionsConfiguration revisionsConfiguration = new RevisionsConfiguration();
        RevisionsCollectionConfiguration defaultCollection = new RevisionsCollectionConfiguration();
        defaultCollection.setPurgeOnDelete(purgeOnDelete);
        defaultCollection.setMinimumRevisionsToKeep(minimumRevisionsToKeep);

        revisionsConfiguration.setDefaultConfig(defaultCollection);
        ConfigureRevisionsOperation operation = new ConfigureRevisionsOperation(revisionsConfiguration);

        return store.maintenance().send(operation);
    }

}
