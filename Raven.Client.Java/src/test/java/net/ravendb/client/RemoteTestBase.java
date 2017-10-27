package net.ravendb.client;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.test.driver.RavenServerLocator;
import net.ravendb.client.test.driver.RavenTestDriver;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;

public class RemoteTestBase extends RavenTestDriver {

    private static class TestServiceLocator extends RavenServerLocator {
        @Override
        public String getServerPath() {
            return "C:\\temp\\RavenDB-4.0.0-nightly-20171020-0400-windows-x64\\Server\\Raven.Server.exe"; //TODO: from variable?
        }
    }

    public RemoteTestBase() {
        super(new TestServiceLocator());
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

        return () -> {
            RequestExecutor.requestPostProcessor = null;
        };
    }

}
