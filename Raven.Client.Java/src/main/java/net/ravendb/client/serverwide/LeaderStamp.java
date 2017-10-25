package net.ravendb.client.serverwide;

public class LeaderStamp {
    private long index;
    private long term;
    private long leadersTicks;

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public long getLeadersTicks() {
        return leadersTicks;
    }

    public void setLeadersTicks(long leadersTicks) {
        this.leadersTicks = leadersTicks;
    }
}
