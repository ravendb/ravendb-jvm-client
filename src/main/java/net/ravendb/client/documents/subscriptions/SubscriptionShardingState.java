package net.ravendb.client.documents.subscriptions;

import java.util.Map;

public class SubscriptionShardingState {

    private Map<String, String> changeVectorForNextBatchStartingPointPerShard;
    private Map<String, String> nodeTagPerShard;
    private Map<Integer, String> processedChangeVectorPerBucket;
    private String changeVectorForNextBatchStartingPointForOrchestrator;

    public Map<String, String> getChangeVectorForNextBatchStartingPointPerShard() {
        return changeVectorForNextBatchStartingPointPerShard;
    }

    public void setChangeVectorForNextBatchStartingPointPerShard(Map<String, String> changeVectorForNextBatchStartingPointPerShard) {
        this.changeVectorForNextBatchStartingPointPerShard = changeVectorForNextBatchStartingPointPerShard;
    }

    public Map<String, String> getNodeTagPerShard() {
        return nodeTagPerShard;
    }

    public void setNodeTagPerShard(Map<String, String> nodeTagPerShard) {
        this.nodeTagPerShard = nodeTagPerShard;
    }

    public Map<Integer, String> getProcessedChangeVectorPerBucket() {
        return processedChangeVectorPerBucket;
    }

    public void setProcessedChangeVectorPerBucket(Map<Integer, String> processedChangeVectorPerBucket) {
        this.processedChangeVectorPerBucket = processedChangeVectorPerBucket;
    }

    public String getChangeVectorForNextBatchStartingPointForOrchestrator() {
        return changeVectorForNextBatchStartingPointForOrchestrator;
    }

    public void setChangeVectorForNextBatchStartingPointForOrchestrator(String changeVectorForNextBatchStartingPointForOrchestrator) {
        this.changeVectorForNextBatchStartingPointForOrchestrator = changeVectorForNextBatchStartingPointForOrchestrator;
    }
}
