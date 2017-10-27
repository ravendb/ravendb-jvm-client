package net.ravendb.client.documents.identity;

import java.util.Date;

/**
 * The result of a NextHiLo operation
 */
public class HiLoResult {

    private String prefix;
    private long low;
    private long high;
    private long lastSize;
    private String serverTag;
    private Date lastRangeAt;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public long getLow() {
        return low;
    }

    public void setLow(long low) {
        this.low = low;
    }

    public long getHigh() {
        return high;
    }

    public void setHigh(long high) {
        this.high = high;
    }

    public long getLastSize() {
        return lastSize;
    }

    public void setLastSize(long lastSize) {
        this.lastSize = lastSize;
    }

    public String getServerTag() {
        return serverTag;
    }

    public void setServerTag(String serverTag) {
        this.serverTag = serverTag;
    }

    public Date getLastRangeAt() {
        return lastRangeAt;
    }

    public void setLastRangeAt(Date lastRangeAt) {
        this.lastRangeAt = lastRangeAt;
    }
}
