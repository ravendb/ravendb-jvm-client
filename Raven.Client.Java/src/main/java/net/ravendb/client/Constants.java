package net.ravendb.client;

public class Constants {

    public static class Headers {
        private Headers() {}


        public static final String REQUEST_TIME = "Raven-Request-Time";

        public static final String REFRESH_TOPOLOGY = "Refresh-Topology";

        public static final String TOPOLOGY_ETAG = "Topology-Etag";

        public static final String CLIENT_CONFIGURATION_ETAG = "Client-Configuration-Etag";

        public static final String REFRESH_CLIENT_CONFIGURATION = "Refresh-Client-Configuration";

        public static final String ETAG = "ETag";

        public static final String IF_NONE_MATCH = "If-None-Match";
        public static final String TRANSFER_ENCODING = "Transfer-Encoding";
        public static final String CONTENT_ENCODING = "Content-Encoding";
        public static final String CONTENT_LENGTH = "Content-Length";

    }
}
