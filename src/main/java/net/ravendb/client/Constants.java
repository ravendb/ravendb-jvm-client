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
            public static final String ID_PROPERTY = "Id";
            public static final String FLAGS = "@flags";
            public static final String ATTACHMENTS = "@attachments";
            public static final String COUNTERS = "@counters";
            public static final String REVISION_COUNTERS = "@counters-snapshot";
            public static final String INDEX_SCORE = "@index-score";
            public static final String LAST_MODIFIED = "@last-modified";
            public static final String RAVEN_JAVA_TYPE = "Raven-Java-Type";
            public static final String CHANGE_VECTOR = "@change-vector";
            public static final String EXPIRES = "@expires";
            public static final String ALL_DOCUMENTS_COLLECTION = "@all_docs";
        }

        public static class Indexing {
            private Indexing() {}

            public static final String SIDE_BY_SIDE_INDEX_NAME_PREFIX = "ReplacementOf/";

            public static class Fields {
                private Fields() {}

                public static final String DOCUMENT_ID_FIELD_NAME = "id()";
                public static final String REDUCE_KEY_HASH_FIELD_NAME = "hash(key())";
                public static final String REDUCE_KEY_KEY_VALUE_FIELD_NAME = "key()";
                public static final String ALL_FIELDS = "__all_fields";
                public static final String SPATIAL_SHAPE_FIELD_NAME = "spatial(shape)";
                //TBD 4.1 public static final String CUSTOM_SORT_FIELD_NAME = "__customSort";
            }

            public static class Spatial {
                private Spatial() {}

                public static final double DEFAULT_DISTANCE_ERROR_PCT = 0.025d;
            }
        }

        public static class PeriodicBackup {

            public static final String FULL_BACKUP_EXTENSION = "ravendb-full-backup";
            public static final String SNAPSHOT_EXTENSION = "ravendb-snapshot";
            public static final String INCREMENTAL_BACKUP_EXTENSION = "ravendb-incremental-backup";

            public static class Folders {
                public static final String INDEXES = "Indexes";
                public static final String DOCUMENTS = "Documents";
                public static final String CONFIGURATION = "Configuration";
            }
        }
    }

    public static class Headers {
        private Headers() {}


        public static final String REQUEST_TIME = "Raven-Request-Time";

        public static final String REFRESH_TOPOLOGY = "Refresh-Topology";

        public static final String TOPOLOGY_ETAG = "Topology-Etag";

        public static final String LAST_KNOWN_CLUSTER_TRANSACTION_INDEX = "Known-Raft-Index";

        public static final String CLIENT_CONFIGURATION_ETAG = "Client-Configuration-Etag";

        public static final String REFRESH_CLIENT_CONFIGURATION = "Refresh-Client-Configuration";

        public static final String CLIENT_VERSION = "Raven-Client-Version";
        public static final String SERVER_VERSION = "Raven-Server-Version";

        public static final String ETAG = "ETag";

        public static final String IF_NONE_MATCH = "If-None-Match";
        public static final String TRANSFER_ENCODING = "Transfer-Encoding";
        public static final String CONTENT_ENCODING = "Content-Encoding";
        public static final String CONTENT_LENGTH = "Content-Length";

    }

    public static class Counters {
        public static final String ALL = "@all_counters";
    }
}
