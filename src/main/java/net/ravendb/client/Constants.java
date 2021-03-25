package net.ravendb.client;

public class Constants {

    public static class Documents{
        private Documents() {}

        public static class Metadata {
            private Metadata() {}

            public static final String COLLECTION = "@collection";
            public static final String PROJECTION = "@projection";
            public static final String KEY = "@metadata";
            public static final String ID = "@id";
            public static final String CONFLICT = "@conflict";
            public static final String LAST_MODIFIED = "@last-modified";
            public static final String RAVEN_JAVA_TYPE = "Raven-Java-Type";
            public static final String CHANGE_VECTOR = "@change-vector";
            public static final String ALL_DOCUMENTS_COLLECTION = "@all_docs";
        }

        public static class Indexing {
            private Indexing() {}

            public static class Fields {
                private Fields() {}

                public static final String DOCUMENT_ID_FIELD_NAME = "id()";
                public static final String REDUCE_KEY_HASH_FIELD_NAME = "hash(key())";
                public static final String REDUCE_KEY_KEY_VALUE_FIELD_NAME = "key()";
                public static final String VALUE_FIELD_NAME = "value()";
                public static final String SPATIAL_SHAPE_FIELD_NAME = "spatial(shape)";
                //TBD 4.1 public static final String CUSTOM_SORT_FIELD_NAME = "__customSort";
            }

        }


    }

    public static class Headers {
        private Headers() {}


        public static final String REFRESH_TOPOLOGY = "Refresh-Topology";

        public static final String TOPOLOGY_ETAG = "Topology-Etag";

        public static final String LAST_KNOWN_CLUSTER_TRANSACTION_INDEX = "Known-Raft-Index";

        public static final String CLIENT_CONFIGURATION_ETAG = "Client-Configuration-Etag";

        public static final String REFRESH_CLIENT_CONFIGURATION = "Refresh-Client-Configuration";

        public static final String CLIENT_VERSION = "Raven-Client-Version";
        public static final String SERVER_VERSION = "Raven-Server-Version";

        public static final String ETAG = "ETag";

    }
}
