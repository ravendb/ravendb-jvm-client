package net.ravendb.client;

public class Constants {

    public final class Json {
        private Json() {}

        public final class Fields {
            private Fields() {}

            public static final String TYPE = "@type";
            public static final String VALUE = "@value";
        }
    }

    public final class QueryString {
        private QueryString() {}

        public static final String NODE_TAG = "nodeTag";
        public static final String SHARD_NUMBER = "shardNumber";
    }

    public static class Headers {
        private Headers() {}

        public static final String REQUEST_TIME = "Raven-Request-Time";
        public static final String SERVER_STARTUP_TIME = "Server-Startup-Time";
        public static final String REFRESH_TOPOLOGY = "Refresh-Topology";
        public static final String TOPOLOGY_ETAG = "Topology-Etag";
        public static final String CLUSTER_TOPOLOGY_ETAG = "Cluster-Topology-Etag";
        public static final String CLIENT_CONFIGURATION_ETAG = "Client-Configuration-Etag";
        public static final String LAST_KNOWN_CLUSTER_TRANSACTION_INDEX = "Known-Raft-Index";
        public static final String DATABASE_CLUSTER_TRANSACTION_ID = "Database-Cluster-Tx-Id";
        public static final String REFRESH_CLIENT_CONFIGURATION = "Refresh-Client-Configuration";
        public static final String ETAG = "ETag";
        public static final String CLIENT_VERSION = "Raven-Client-Version";
        public static final String SERVER_VERSION = "Raven-Server-Version";
        public static final String STUDIO_VERSION = "Raven-Studio-Version";
        public static final String IF_MATCH = "If-Match";
        public static final String IF_NONE_MATCH = "If-None-Match";
        public static final String TRANSFER_ENCODING = "Transfer-Encoding";
        public static final String CONTENT_ENCODING = "Content-Encoding";
        public static final String ACCEPT_ENCODING = "Accept-Encoding";
        public static final String CONTENT_DISPOSITION = "Content-Disposition";
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String CONTENT_LENGTH = "Content-Length";
        public static final String ORIGIN = "Origin";
        public static final String INCREMENTAL_TIME_SERIES_PREFIX = "INC:";
        public static final String SHARDED = "Sharded";
        public static final String ATTACHMENT_HASH = "Attachment-Hash";
        public static final String ATTACHMENT_SIZE = "Attachment-Size";
        public static final String DATABASE_MISSING = "Database-Missing";

        public static class Encodings {
            private Encodings() {}
            public static final String GZIP = "gzip";
            public static final String BROTLI = "br";
            public static final String DEFLATE = "deflate";
            public static final String ZSTD = "zstd";

        }
    }

    public final class Platform {
        private Platform() {}

        public final class Windows {
            private Windows() {}

            public static final int MAX_PATH = Short.MAX_VALUE;
            public final String[] RESERVED_FILE_NAMES = {
                    "con",
                    "prn",
                    "aux",
                    "nul",
                    "com1",
                    "com2",
                    "com3",
                    "com4",
                    "com5",
                    "com6",
                    "com7",
                    "com8",
                    "com9",
                    "lpt1",
                    "lpt2",
                    "lpt3",
                    "lpt4",
                    "lpt5",
                    "lpt6",
                    "lpt7",
                    "lpt8",
                    "lpt9",
                    "clock$"
            };
        }

        public final class Linux {
            private Linux() {}

            public static final int MAX_PATH = 4096;
            public static final int MAX_FILE_NAME_LENGTH = 230;
        }
    }

    public static final class Certificates {
        private Certificates() {}

        public static final String PREFIX = "certificates/";
        public static final Integer MAX_NUMBER_OF_CERTS_WITH_SAME_HASH = 5;
    }

    public static final class Network {
        public static final String ANY_IP = "0.0.0.0";
        public static final Integer ZERO_VALUE = 0;
        public static final Integer DEFAULT_SECURED_RAVENDB_HTTP_PORT = 443;
        public static final Integer DEFAULT_SECURED_RAVENDB_TCP_PORT = 38888;
    }

    public final class DatabaseSettings {
        private DatabaseSettings() {}

        public static final String STUDIO_ID = "DatabaseSettings/Studio";
    }

    public static class Configuration {
        private Configuration() {}

        public static class Indexes {
            public static final String INDEXING_STATIC_SEARCH_ENGINE_TYPE = "Indexing.Static.SearchEngineType";
            public static final String INDEXING_AUTO_SEARCH_ENGINE_TYPE = "Indexing.Auto.SearchEngineType";
        }

        public final String CLIENT_ID = "Configuration/Client";
        public final String STUDIO_ID = "Configuration/Studio";
    }

    public static class Counters {
        public static final String ALL = "@all_counters";
    }

    public static class TimeSeries {
        public static final String SELECT_FIELD_NAME = "timeseries";
        public static final String QUERY_FUNCTION = "__timeSeriesQueryFunction";
        public static final String ALL = "@all_timeseries";
    }

    public static class Documents {
        private Documents() {}

        public static final String PREFIX = "db/";
        public static final Integer MAX_DATABASE_NAME_LENGTH = 128;

        public enum SubscriptionChangeVectorSpecialStates{
            DO_NOT_CHANGE,
            LAST_DOCUMENT,
            BEGINNING_OF_TIME
        }

        public static class Metadata {
            private Metadata() {}

            public static final String EDGES = "@edges";
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
            public static final String TIME_SERIES_NAMED_VALUE = "@timeseries-named-values";
            public static final String REVISION_COUNTERS = "@counters-snapshot";
            public static final String REVISION_TIME_SERIES = "@timeseries-snapshot";
            public static final String LEGACY_ATTACHMENTS_METADATA = "@legacy-attachment-metadata";
            public static final String INDEX_SCORE = "@index-score";
            public static final String SPATIAL_RESULT = "@spatial";
            public static final String LAST_MODIFIED = "@last-modified";
            public static final String RAVEN_JAVA_TYPE = "Raven-Java-Type";
            public static final String CHANGE_VECTOR = "@change-vector";
            public static final String EXPIRES = "@expires";
            public static final String REFRESH = "@refresh";
            public static final String ARCHIVE_AT = "@archive-at";
            public static final String ARCHIVED = "@archived";
            public static final String HAS_VALUE = "HasValue";
            public static final String ETAG = "@etag";

            public static final class Sharding {
                public static final String SHARD_NUMBER = "@shard-number";

                public static final class Querying {
                    public static final String ORDER_BY_FIELDS = "@order-by-fields";
                    public static final String SUGGESTION_POPULARITY_FIELDS = "@suggestions-popularity";
                    public static final String RESULT_DATA_HASH = "@data-hash";
                }

                public static final class Subscription {
                    public static final String NON_PERSISTENT_FLAGS = "@non-persistent-flags";
                }
            }
        }

        public static final class Collections {
            public static final String ALL_DOCUMENTS_COLLECTION = "@all_docs";
            public static final String EMPTY_COLLECTION = "@empty";
        }

        public static class Indexing {
            private Indexing() {}

            public static final String SIDE_BY_SIDE_INDEX_NAME_PREFIX = "ReplacementOf/";

            public static class Fields {
                private Fields() {}

                public static final String COUNT_FIELD_NAME = "Count";
                public static final String CUSTOM_SORT_FIELD_NAME = "__customSort";
                public static final String DOCUMENT_ID_FIELD_NAME = "id()";
                public static final String DOCUMENT_ID_METHOD_NAME = "id";
                public static final String SOURCE_DOCUMENT_ID_FIELD_NAME = "sourceDocId()";
                public static final String REDUCE_KEY_HASH_FIELD_NAME = "hash(key())";
                public static final String REDUCE_KEY_KEY_VALUE_FIELD_NAME = "key()";
                public static final String VALUE_FIELD_NAME = "value()";
                public static final String ALL_FIELDS = "__all_fields";
                public static final String ALL_STORED_FIELDS = "__all_stored_fields";
                public static final String SPATIAL_SHAPE_FIELD_NAME = "spatial(shape)";
                public static final String RANGE_FIELD_SUFFIX = "_Range";
                public static final String RANGE_FIELD_SUFFIX_LONG = "_L" + RANGE_FIELD_SUFFIX;
                public static final String RangeFieldSuffixDouble = "_D" + RANGE_FIELD_SUFFIX;
                public static final String TIME_FIELD_SUFFIX = "_Time";
                public static final String NULL_VALUE = "NULL_VALUE";
                public static final String EMPTY_STRING = "EMPTY_STRING";

                public static final class JavaScript {
                    private JavaScript() {}

                    public static final String VALUE_PROPERTY_NAME = "$value";
                    public static final String OPTIONS_PROPERTY_NAME = "$options";
                    public static final String NAME_PROPERTY_NAME = "$name";
                    public static final String SPATIAL_PROPERTY_NAME = "$spatial";
                    public static final String BOOST_PROPERTY_NAME = "$boost";
                }
            }

            public static class Spatial {
                private Spatial() {}

                public static final double DEFAULT_DISTANCE_ERROR_PCT = 0.025d;
                public static final Double EARTH_MEAN_RADIUS_KM = 6371.0087714;
                public static final Double MILES_TO_KM = 1.60934;
            }
        }

        public static final class Analyzers {
            private Analyzers() {}

            public static final String DEFAULT = "LowerCaseKeywordAnalyzer";
            public static final String DEFAULT_EXACT = "KeywordAnalyzer";
            public static final String DEFAULT_SEARCH = "RavenStandardAnalyzer";
        }

        public static final class Querying {
            private Querying() {}

            public static final class Facet {
                private Facet() {}

                public static final String ALL_RESULTS = "@AllResults";
            }

            public static final class Fields {
                private Fields() {}

                public static final String POWER_BI_JSON_FIELD_NAME = "json()";
            }

            public static class Sharding {
                private Sharding() {}

                public static final String SHARD_CONTEXT_PARAMETER_NAME = "__shardContext";
            }
        }

        public static class PeriodicBackup {

            public static final String FULL_BACKUP_EXTENSION = ".ravendb-full-backup";
            public static final String SNAPSHOT_EXTENSION = ".ravendb-snapshot";
            public static final String ENCRYPTED_FULL_BACKUP_EXTENSION = ".ravendb-encrypted-full-backup";
            public static final String ENCRYPTED_SNAPSHOT_EXTENSION = ".ravendb-encrypted-snapshot";
            public static final String INCREMENTAL_BACKUP_EXTENSION = ".ravendb-incremental-backup";
            public static final String ENCRYPTED_INCREMENTAL_BACKUP_EXTENSION = ".ravendb-encrypted-incremental-backup";

            public static class Folders {
                public static final String INDEXES = "Indexes";
                public static final String DOCUMENTS = "Documents";
                public static final String CONFIGURATION = "Configuration";
            }
        }

        public static final class Blob {
            public static final String DOCUMENT = "@raven-data";
            public static final String SIZE = "@raven-blob-size";
        }
    }

    public static final class Identities {
        public static final Character DEFAULT_SEPARATOR = '/';
    }

    public static final class Smuggler
    {
        public static final String IMPORT_OPTIONS = "importOptions";
        public static final String CSV_IMPORT_OPTIONS = "csvImportOptions";
    }

    public static final class Operations {
        public static final Long INVALID_OPERATION_ID = -1L;
    }

    public static class CompareExchange {
        private CompareExchange() {}

        public static final String RVN_ATOMIC_PREFIX = "rvn-atomic/";
        public static final String OBJECT_FIELD_NAME = "Object";
    }

    public static final class Monitoring {
        private Monitoring() {}

        public static final class Snmp {
            private Snmp() {}

            public static final String DATABASES_MAPPING_KEY = "monitoring/snmp/databases/mapping";
        }
    }

    public static final class Fields {
        private Fields() {}

        public static final class CommandData {
            private CommandData() {}

            public static final String DOCUMENT_CHANGE_VECTOR = null;
            public static final String DESTINATION_DOCUMENT_CHANGE_VECTOR = null;
        }
    }

    public static final class Obsolete {
        private Obsolete() {}
    }

    public static final class DatabaseRecord {
        private DatabaseRecord() {}

        public static final class SupportedFeatures {
            private SupportedFeatures() {}

            public static final String THROW_REVISION_KEY_TOO_BIG_FIX = "ThrowRevisionKeyTooBigFix";
        }
    }
}
