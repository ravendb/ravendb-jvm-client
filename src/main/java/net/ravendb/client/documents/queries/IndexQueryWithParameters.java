package net.ravendb.client.documents.queries;

public abstract class IndexQueryWithParameters<T> extends IndexQueryBase<T> {

    private boolean skipDuplicateChecking;

    //TBD 4.1 private boolean explainScores;

    //TBD 4.1 private boolean showTimings;

    /**
     * Allow to skip duplicate checking during queries
     * @return true if server can skip duplicate checking
     */
    public boolean isSkipDuplicateChecking() {
        return skipDuplicateChecking;
    }

    /**
     * Allow to skip duplicate checking during queries
     * @param skipDuplicateChecking sets the value
     */
    public void setSkipDuplicateChecking(boolean skipDuplicateChecking) {
        this.skipDuplicateChecking = skipDuplicateChecking;
    }

    //TBD 4.1 public boolean isExplainScores() {

    //TBD 4.1 public void setExplainScores(boolean explainScores) {

    //TBD 4.1 public boolean isShowTimings()
    //TBD 4.1 public void setShowTimings(boolean showTimings) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IndexQueryWithParameters<?> that = (IndexQueryWithParameters<?>) o;

        return skipDuplicateChecking == that.skipDuplicateChecking;
        //TBD 4.1 return showTimings == that.showTimings;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (skipDuplicateChecking ? 1 : 0);
        //TBD 4.1 result = 31 * result + (showTimings ? 1 : 0);
        return result;
    }
}
