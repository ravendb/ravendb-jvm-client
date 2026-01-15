package net.ravendb.client.documents.operations.etl.elasticSearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticSearchConnectionString extends ConnectionString {

    private String[] nodes;

    private Authentication authentication;

    @Override
    protected void validateImpl(List<String> errors) {

        if (nodes == null || nodes.length == 0) {
            errors.add("Nodes cannot be empty");
        }
        if (nodes == null) {
            return;
        }

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] == null) {
                errors.add("Url number " + (i + 1) + " in Nodes cannot be empty");
                continue;
            }

            nodes[i] = nodes[i].trim();
        }
    }

    @Override
    public ConnectionStringType getType() {
        return ConnectionStringType.ELASTIC_SEARCH;
    }

    private boolean enableCompatibilityMode;

    public String[] getNodes() {
        return nodes;
    }

    public void setNodes(String[] nodes) {
        this.nodes = nodes;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    /**
     * @deprecated Elasticsearch compatibility isn't required anymore to connect with Elasticsearch server v8.x.
     */
    public boolean isEnableCompatibilityMode() {
        return enableCompatibilityMode;
    }

    /**
     * @deprecated Elasticsearch compatibility isn't required anymore to connect with Elasticsearch server v8.x.
     */
    public void setEnableCompatibilityMode(boolean enableCompatibilityMode) {
        this.enableCompatibilityMode = enableCompatibilityMode;
    }

    public static class Authentication {
        private ApiKeyAuthentication apiKey;
        private BasicAuthentication basic;
        private CertificateAuthentication certificate;

        public ApiKeyAuthentication getApiKey() {
            return apiKey;
        }

        public void setApiKey(ApiKeyAuthentication apiKey) {
            this.apiKey = apiKey;
        }

        public BasicAuthentication getBasic() {
            return basic;
        }

        public void setBasic(BasicAuthentication basic) {
            this.basic = basic;
        }

        public CertificateAuthentication getCertificate() {
            return certificate;
        }

        public void setCertificate(CertificateAuthentication certificate) {
            this.certificate = certificate;
        }
    }

    public static class ApiKeyAuthentication {
        private String apiKeyId;
        private String apiKey;

        public String getApiKeyId() {
            return apiKeyId;
        }

        public void setApiKeyId(String apiKeyId) {
            this.apiKeyId = apiKeyId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class BasicAuthentication {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class CertificateAuthentication {
        private String[] certificatesBase64;

        public String[] getCertificatesBase64() {
            return certificatesBase64;
        }

        public void setCertificatesBase64(String[] certificatesBase64) {
            this.certificatesBase64 = certificatesBase64;
        }
    }
}
