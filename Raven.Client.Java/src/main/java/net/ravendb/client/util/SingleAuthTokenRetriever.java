package net.ravendb.client.util;

import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.connection.CreateHttpJsonRequestParams;
import net.ravendb.client.connection.OperationMetadata;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.connection.profiling.IHoldProfilingInformation;
import net.ravendb.client.document.Convention;

import java.util.Map;

public class SingleAuthTokenRetriever {
    private final IHoldProfilingInformation profilingInformation;
    private final HttpJsonRequestFactory factory;
    private final Convention convention;
    private final Map<String, String> operationHeaders;
    private final OperationMetadata operationMetadata;

    public SingleAuthTokenRetriever(IHoldProfilingInformation profilingInformation, HttpJsonRequestFactory factory, Convention convention, Map<String, String> operationHeaders, OperationMetadata operationMetadata) {
        this.profilingInformation = profilingInformation;
        this.factory = factory;
        this.convention = convention;
        this.operationHeaders = operationHeaders;
        this.operationMetadata = operationMetadata;
    }

    public String getToken() {
        try (HttpJsonRequest request = createRequestParams(operationMetadata, "/singleAuthToken", HttpMethods.GET, true, false)) {
            RavenJToken response = request.readResponseJson();
            return  response.value(String.class, "Token");
        }
    }

    public String validateThatWeCanUseToken(String token) {
        try (HttpJsonRequest request = createRequestParams(operationMetadata, "/singleAuthToken", HttpMethods.GET, true, true)) {
            request.addOperationHeader("Single-Use-Auth-Token", token);
            RavenJToken response = request.readResponseJson();
            return  response.value(String.class, "Token");
        }
    }

    private HttpJsonRequest createRequestParams(OperationMetadata operationMetadata, String requestUrl, HttpMethods method,
                                                boolean disableRequestCompression, boolean disableAuthentication) {
        RavenJObject metadata = new RavenJObject();
        CreateHttpJsonRequestParams createHttpJsonRequestParams = new CreateHttpJsonRequestParams(profilingInformation, operationMetadata.getUrl() + requestUrl, method, metadata, operationMetadata.getCredentials(), convention);
        createHttpJsonRequestParams.addOperationHeaders(operationHeaders);

        createHttpJsonRequestParams.setDisableRequestCompression(disableRequestCompression);
        createHttpJsonRequestParams.setDisableAuthentication(disableAuthentication);

        return factory.createHttpJsonRequest(createHttpJsonRequestParams);
    }
}
