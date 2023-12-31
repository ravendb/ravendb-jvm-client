package net.ravendb.client.serverwide.sharding;

import java.util.ArrayList;
import java.util.List;

public class ShardBucketMigration {

    private MigrationStatus status;
    private int bucket;
    private int sourceShard;
    private int destinationShard;
    private long migrationIndex;
    private Long confirmationIndex;
    private String lastSourceChangeVector;

    private List<String> confirmedDestinations = new ArrayList<>();
    private List<String> confirmedSourceCleanup = new ArrayList<>();

    private String mentorNode;

    public MigrationStatus getStatus() {
        return status;
    }

    public void setStatus(MigrationStatus status) {
        this.status = status;
    }

    public int getBucket() {
        return bucket;
    }

    public void setBucket(int bucket) {
        this.bucket = bucket;
    }

    public int getSourceShard() {
        return sourceShard;
    }

    public void setSourceShard(int sourceShard) {
        this.sourceShard = sourceShard;
    }

    public int getDestinationShard() {
        return destinationShard;
    }

    public void setDestinationShard(int destinationShard) {
        this.destinationShard = destinationShard;
    }

    public long getMigrationIndex() {
        return migrationIndex;
    }

    public void setMigrationIndex(long migrationIndex) {
        this.migrationIndex = migrationIndex;
    }

    public Long getConfirmationIndex() {
        return confirmationIndex;
    }

    public void setConfirmationIndex(Long confirmationIndex) {
        this.confirmationIndex = confirmationIndex;
    }

    public String getLastSourceChangeVector() {
        return lastSourceChangeVector;
    }

    public void setLastSourceChangeVector(String lastSourceChangeVector) {
        this.lastSourceChangeVector = lastSourceChangeVector;
    }

    public List<String> getConfirmedDestinations() {
        return confirmedDestinations;
    }

    public void setConfirmedDestinations(List<String> confirmedDestinations) {
        this.confirmedDestinations = confirmedDestinations;
    }

    public List<String> getConfirmedSourceCleanup() {
        return confirmedSourceCleanup;
    }

    public void setConfirmedSourceCleanup(List<String> confirmedSourceCleanup) {
        this.confirmedSourceCleanup = confirmedSourceCleanup;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }
}
