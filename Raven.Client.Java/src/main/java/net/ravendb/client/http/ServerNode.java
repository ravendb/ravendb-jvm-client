package net.ravendb.client.http;

public class ServerNode {

    public enum Role {
        NONE,
        PROMOTABLE,
        MEMBER,
        REHAB
    }

    private String url;
    private String database;
    private String clusterTag;
    private Role serverRole;

    public ServerNode() {
        /* TODO
        for (var i = 0; i < 60; i++)
                UpdateRequestTime(0);
         */
    }

    public void updateRequestTime(long requestTimeInMilliseconds) {
        /*
        _ewma.Update(requestTimeInMilliseconds);
            _ewma.Tick();
         */
    }

    public boolean isRateSurpassed(double requestTimeSlaThresholdInMilliseconds) {
        return true; //TODO: delete me
        /* TODO
          var rate = Rate();

            if (_isRateSurpassed)
                return _isRateSurpassed = rate >= SwitchBackRatio * requestTimeSlaThresholdInMilliseconds;

            return _isRateSurpassed = rate >= requestTimeSlaThresholdInMilliseconds;
         */
    }

    public double rate() {
        return 0;
        //TODO: return _ewma.Rate(TimeUnit.Milliseconds);
    }

    public void decreaseRate(long requestTimeInMilliseconds) {
        /* TODO:
           var rate = Rate();
            var maxRate = MaxDecreasingRatio * rate;
            var minRate = MinDecreasingRatio * rate;

            var decreasingRate = rate - requestTimeInMilliseconds;

            if (decreasingRate > maxRate)
                decreasingRate = maxRate;

            if (decreasingRate < minRate)
                decreasingRate = minRate;

            UpdateRequestTime((long)decreasingRate);
         */
    }

    /* TODO:
    private readonly EWMA _ewma = new EWMA(EWMA.M1Alpha, 1, TimeUnit.Milliseconds);
        private const double SwitchBackRatio = 0.75;
        private bool _isRateSurpassed;

        private const double MaxDecreasingRatio = 0.75;
        private const double MinDecreasingRatio = 0.25;

     */

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getClusterTag() {
        return clusterTag;
    }

    public void setClusterTag(String clusterTag) {
        this.clusterTag = clusterTag;
    }

    public Role getServerRole() {
        return serverRole;
    }

    public void setServerRole(Role serverRole) {
        this.serverRole = serverRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServerNode that = (ServerNode) o;

        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        return database != null ? database.equals(that.database) : that.database == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (database != null ? database.hashCode() : 0);
        return result;
    }

}
