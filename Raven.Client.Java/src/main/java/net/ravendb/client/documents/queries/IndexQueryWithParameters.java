package net.ravendb.client.documents.queries;

public abstract class IndexQueryWithParameters<T> extends IndexQueryBase<T> {

    private boolean skipDuplicateChecking;

    //TBD private boolean explainScores;

    //TBD private boolean showTimings;

    /**
     * Allow to skip duplicate checking during queries
     */
    public boolean isSkipDuplicateChecking() {
        return skipDuplicateChecking;
    }

    /**
     * Allow to skip duplicate checking during queries
     */
    public void setSkipDuplicateChecking(boolean skipDuplicateChecking) {
        this.skipDuplicateChecking = skipDuplicateChecking;
    }

    //TBD public boolean isExplainScores() {

    //TBD public void setExplainScores(boolean explainScores) {

    //TBD public boolean isShowTimings()
    //TBD public void setShowTimings(boolean showTimings) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IndexQueryWithParameters<?> that = (IndexQueryWithParameters<?>) o;

        return skipDuplicateChecking == that.skipDuplicateChecking;
        //TBD return showTimings == that.showTimings;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (skipDuplicateChecking ? 1 : 0);
        //TBD result = 31 * result + (showTimings ? 1 : 0);
        return result;
    }
}
