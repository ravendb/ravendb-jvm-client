package net.ravendb.client.documents.indexes;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum IndexType {
    NONE,
    AUTO_MAP,
    AUTO_MAP_REDUCE,
    MAP,
    MAP_REDUCE,
    FAULTY,
    JAVA_SCRIPT_MAP,
    JAVA_SCRIPT_MAP_REDUCE
}
