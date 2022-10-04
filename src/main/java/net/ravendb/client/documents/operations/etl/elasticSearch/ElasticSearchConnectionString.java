package net.ravendb.client.documents.operations.etl.elasticSearch;

import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;

public class ElasticSearchConnectionString extends ConnectionString {

    private String[] nodes;

    private Authentication authentication;

    @Override
    public ConnectionStringType getType() {
        return ConnectionStringType.ELASTIC_SEARCH;
    }

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
