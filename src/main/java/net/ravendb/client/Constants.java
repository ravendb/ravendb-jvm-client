package net.ravendb.client;

public class Constants {

    public static class DatabaseRecord {
        private DatabaseRecord() {}

        public static class SupportedFeatures {
            private SupportedFeatures() {}

            public static final String THROW_REVISION_KEY_TOO_BIG_FIX = "ThrowRevisionKeyTooBigFix";
            public static final String THROW_CONTROL_CHARACTERS_IN_IDENTIFIER = "ThrowControlCharactersInIdentifier";
        }
    }

    public static class AI {
        private AI() {}
        public static final String PROMPT_TOKENS = "prompt_tokens";
        public static final String COMPLETION_TOKENS = "completion_tokens";
        public static final String TOTAL_TOKENS = "total_tokens";
        public static final String PROMPT_TOKENS_DETAILS = "prompt_tokens_details";
        public static final String COMPLETION_TOKENS_DETAILS = "completion_tokens_details";
        public static final String CACHED_TOKENS = "cached_tokens";
        public static final String REASONING_TOKENS = "reasoning_tokens";
    }

    public static class Documents {
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
            public static final String TIME_SERIES = "@timeseries";
            public static final String REVISION_COUNTERS = "@counters-snapshot";
            public static final String REVISION_TIME_SERIES = "@timeseries-snapshot";
            public static final String INDEX_SCORE = "@index-score";
            public static final String LAST_MODIFIED = "@last-modified";
            public static final String RAVEN_JAVA_TYPE = "Raven-Java-Type";
            public static final String CHANGE_VECTOR = "@change-vector";
            public static final String EXPIRES = "@expires";
            public static final String REFRESH = "@refresh";
            public static final String ARCHIVE_AT = "@archive-at";
            public static final String ARCHIVED = "@archived";
            public static final String ALL_DOCUMENTS_COLLECTION = "@all_docs";

            public static final String EMPTY_COLLECTION = "@empty";

        }

        public static class Indexing {
            private Indexing() {}

            public static final String SIDE_BY_SIDE_INDEX_NAME_PREFIX = "ReplacementOf/";

            public static class Fields {
                private Fields() {}

                public static final String DOCUMENT_ID_FIELD_NAME = "id()";
                public static final String SOURCE_DOCUMENT_ID_FIELD_NAME = "sourceDocId()";
                public static final String REDUCE_KEY_HASH_FIELD_NAME = "hash(key())";
                public static final String REDUCE_KEY_KEY_VALUE_FIELD_NAME = "key()";
                public static final String VALUE_FIELD_NAME = "value()";
                public static final String ALL_FIELDS = "__all_fields";
                public static final String SPATIAL_SHAPE_FIELD_NAME = "spatial(shape)";
                //TBD 4.1 public static final String CUSTOM_SORT_FIELD_NAME = "__customSort";
            }

            public static class Spatial {
                private Spatial() {}

                public static final double DEFAULT_DISTANCE_ERROR_PCT = 0.025d;
            }
        }

        public static class Querying {
            private Querying() {}

            public static class Sharding {
                private Sharding() {}

                public static final String SHARD_CONTEXT_PARAMETER_NAME = "__shardContext";
            }
        }

        public static class PeriodicBackup {

            public static final String FULL_BACKUP_EXTENSION = "ravendb-full-backup";
            public static final String SNAPSHOT_EXTENSION = "ravendb-snapshot";
            public static final String ENCRYPTED_FULL_BACKUP_EXTENSION = ".ravendb-encrypted-full-backup";
            public static final String ENCRYPTED_SNAPSHOT_EXTENSION = ".ravendb-encrypted-snapshot";
            public static final String INCREMENTAL_BACKUP_EXTENSION = "ravendb-incremental-backup";
            public static final String ENCRYPTED_INCREMENTAL_BACKUP_EXTENSION = ".ravendb-encrypted-incremental-backup";

            public static class Folders {
                public static final String INDEXES = "Indexes";
                public static final String DOCUMENTS = "Documents";
                public static final String CONFIGURATION = "Configuration";
            }
        }
    }

    public static class QueryString {
        private QueryString() {}

        public static final String NODE_TAG = "nodeTag";
        public static final String SHARD_NUMBER = "shardNumber";
    }

    public static class Headers {
        private Headers() {}


        public static final String REQUEST_TIME = "Raven-Request-Time";

        public static final String REFRESH_TOPOLOGY = "Refresh-Topology";

        public static final String TOPOLOGY_ETAG = "Topology-Etag";
        public static final String CLUSTER_TOPOLOGY_ETAG = "Cluster-Topology-Etag";

        public static final String LAST_KNOWN_CLUSTER_TRANSACTION_INDEX = "Known-Raft-Index";

        public static final String CLIENT_CONFIGURATION_ETAG = "Client-Configuration-Etag";

        public static final String REFRESH_CLIENT_CONFIGURATION = "Refresh-Client-Configuration";

        public static final String CLIENT_VERSION = "Raven-Client-Version";
        public static final String SERVER_VERSION = "Raven-Server-Version";

        public static final String ETAG = "ETag";

        public static final String IF_MATCH = "If-Match";
        public static final String IF_NONE_MATCH = "If-None-Match";
        public static final String TRANSFER_ENCODING = "Transfer-Encoding";
        public static final String CONTENT_ENCODING = "Content-Encoding";

        public static final String ACCEPT_ENCODING = "Accept-Encoding";
        public static final String CONTENT_DISPOSITION = "Content-Disposition";

        public static final String CONTENT_TYPE = "Content-Type";

        public static final String CONTENT_LENGTH = "Content-Length";
        public static final String INCREMENTAL_TIME_SERIES_PREFIX = "INC:";

        public static final String ORIGIN = "Origin";

        public static final String SHARDED = "Sharded";

        public static final String ATTACHMENT_HASH = "Attachment-Hash";

        /**
         * HTTP header name for the scheduled remote attachment upload time.
         *
         * <p>
         * This header contains the UTC timestamp indicating when an attachment should be uploaded
         * to remote cloud storage.
         * </p>
         *
         * <p>
         * It is used together with {@link #ATTACHMENT_REMOTE_PARAMETERS_FLAGS} and
         * {@link #ATTACHMENT_REMOTE_PARAMETERS_IDENTIFIER} to configure automatic offloading of
         * attachments to cloud storage providers such as Amazon S3 or Azure Blob Storage.
         * </p>
         */
        public static final String ATTACHMENT_REMOTE_PARAMETERS_AT = "Attachment-RemoteParameters-At";

        /**
         * HTTP header name for remote attachment configuration flags.
         *
         * <p>
         * This header contains flags that control remote attachment behavior, such as whether
         * the attachment should be uploaded immediately or at a scheduled time.
         * </p>
         *
         * <p>
         * It is used together with {@link #ATTACHMENT_REMOTE_PARAMETERS_AT} and
         * {@link #ATTACHMENT_REMOTE_PARAMETERS_IDENTIFIER} to specify the complete remote storage
         * configuration. The flags correspond to the values of the
         * {@link net.ravendb.client.documents.attachments.RemoteAttachmentFlags} enum.
         * </p>
         */
        public static final String ATTACHMENT_REMOTE_PARAMETERS_FLAGS = "Attachment-RemoteParameters-Flags";

        /**
         * HTTP header name for the remote attachment storage destination identifier.
         *
         * <p>
         * This header contains the unique identifier of the remote storage destination
         * configuration where the attachment should be uploaded. The identifier refers to a
         * configured remote attachment destination (for example, a specific S3 bucket or Azure
         * Blob Storage container) defined in the database's remote attachments configuration.
         * </p>
         *
         * <p>
         * It is used together with {@link #ATTACHMENT_REMOTE_PARAMETERS_AT} and
         * {@link #ATTACHMENT_REMOTE_PARAMETERS_FLAGS} to specify the complete remote storage
         * parameters for an attachment.
         * </p>
         */
        public static final String ATTACHMENT_REMOTE_PARAMETERS_IDENTIFIER = "Attachment-RemoteParameters-Identifier";

        public static final String ATTACHMENT_SIZE = "Attachment-Size";

        public static final String DATABASE_MISSING = "Database-Missing";


        public static class Encodings {
            public static final String GZIP = "gzip";

            public static final String ZSTD = "zstd";
        }

    }

    public static class Configuration {
        public static class Indexes {
            public static final String INDEXING_STATIC_SEARCH_ENGINE_TYPE = "Indexing.Static.SearchEngineType";
        }
    }

    public static class Counters {
        public static final String ALL = "@all_counters";
    }

    public static class TimeSeries {
        public static final String SELECT_FIELD_NAME = "timeseries";
        public static final String QUERY_FUNCTION = "__timeSeriesQueryFunction";

        public static final String ALL = "@all_timeseries";
    }

    public static class CompareExchange {
        public static final String RVN_ATOMIC_PREFIX = "rvn-atomic/";
        public static final String OBJECT_FIELD_NAME = "Object";
    }

    public static class Indexes {
        public static final String INDEXING_STATIC_SEARCH_ENGINE_TYPE = "Indexing.Static.SearchEngineType";
        public static final String INDEXING_AUTO_SEARCH_ENGINE_TYPE = "Indexing.Auto.SearchEngineType";
    }

    public static class Identities {
        public static final Character DEFAULT_SEPARATOR = '/';
    }

}
