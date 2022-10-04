package net.ravendb.client.serverwide.operations;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum  DatabasePromotionStatus {
    WAITING_FOR_FIRST_PROMOTION,
    NOT_RESPONDING,
    INDEX_NOT_UP_TO_DATE,
    CHANGE_VECTOR_NOT_MERGED,
    WAITING_FOR_RESPONSE,
    OK,
    OUT_OF_CPU_CREDITS,
    EARLY_OUT_OF_MEMORY,
    HIGH_DIRTY_MEMORY,
    RAFT_INDEX_NOT_UP_TO_DATE
}
