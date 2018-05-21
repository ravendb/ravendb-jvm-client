package net.ravendb.client.documents.queries.moreLikeThis;

public class MoreLikeThisOptions {

    public final static int DEFAULT_MAXIMUM_NUMBER_OF_TOKENS_PARSED = 5000;
    public final static int DEFAULT_MINIMUM_TERM_FREQUENCY = 2;
    public final static int DEFAULT_MINIMUM_DOCUMENT_FREQUENCY = 5;
    public final static int DEFAULT_MAXIMUM_DOCUMENT_FREQUENCY = Integer.MAX_VALUE;
    public final static boolean DEFAULT_BOOST = false;
    public final static float DEFAULT_BOOST_FACTOR = 1;
    public final static int DEFAULT_MINIMUM_WORD_LENGTH = 0;
    public final static int DEFAULT_MAXIMUM_WORD_LENGTH = 0;
    public final static int DEFAULT_MAXIMUM_QUERY_TERMS = 25;

    public static MoreLikeThisOptions defaultOptions = new MoreLikeThisOptions();

    private Integer minimumTermFrequency;
    private Integer maximumQueryTerms;
    private Integer maximumNumberOfTokensParsed;
    private Integer minimumWordLength;
    private Integer maximumWordLength;
    private Integer minimumDocumentFrequency;
    private Integer maximumDocumentFrequency;
    private Integer maximumDocumentFrequencyPercentage;
    private Boolean boost;
    private Float boostFactor;
    private String stopWordsDocumentId;
    private String[] fields;

    /**
     * @return Ignore terms with less than this frequency in the source doc. Default is 2.
     */
    public Integer getMinimumTermFrequency() {
        return minimumTermFrequency;
    }

    /**
     * @param minimumTermFrequency Ignore terms with less than this frequency in the source doc. Default is 2.
     */
    public void setMinimumTermFrequency(Integer minimumTermFrequency) {
        this.minimumTermFrequency = minimumTermFrequency;
    }

    /**
     * @return Return a Query with no more than this many terms. Default is 25.
     */
    public Integer getMaximumQueryTerms() {
        return maximumQueryTerms;
    }

    /**
     * @param maximumQueryTerms Return a Query with no more than this many terms. Default is 25.
     */
    public void setMaximumQueryTerms(Integer maximumQueryTerms) {
        this.maximumQueryTerms = maximumQueryTerms;
    }

    /**
     * @return The maximum number of tokens to parse in each example doc field that is not stored with TermVector support. Default is 5000.
     */

    public Integer getMaximumNumberOfTokensParsed() {
        return maximumNumberOfTokensParsed;
    }

    /**
     * @param maximumNumberOfTokensParsed The maximum number of tokens to parse in each example doc field that is not stored with TermVector support. Default is 5000.
     */
    public void setMaximumNumberOfTokensParsed(Integer maximumNumberOfTokensParsed) {
        this.maximumNumberOfTokensParsed = maximumNumberOfTokensParsed;
    }

    /**
     * @return Ignore words less than this length or if 0 then this has no effect. Default is 0.
     */
    public Integer getMinimumWordLength() {
        return minimumWordLength;
    }

    /**
     * @param minimumWordLength Ignore words less than this length or if 0 then this has no effect. Default is 0.
     */
    public void setMinimumWordLength(Integer minimumWordLength) {
        this.minimumWordLength = minimumWordLength;
    }

    /**
     * @return Ignore words greater than this length or if 0 then this has no effect. Default is 0.
     */
    public Integer getMaximumWordLength() {
        return maximumWordLength;
    }

    /**
     * @param maximumWordLength Ignore words greater than this length or if 0 then this has no effect. Default is 0.
     */
    public void setMaximumWordLength(Integer maximumWordLength) {
        this.maximumWordLength = maximumWordLength;
    }

    /**
     * @return Ignore words which do not occur in at least this many documents. Default is 5.
     */
    public Integer getMinimumDocumentFrequency() {
        return minimumDocumentFrequency;
    }

    /**
     * @param minimumDocumentFrequency Ignore words which do not occur in at least this many documents. Default is 5.
     */
    public void setMinimumDocumentFrequency(Integer minimumDocumentFrequency) {
        this.minimumDocumentFrequency = minimumDocumentFrequency;
    }

    /**
     * @return Ignore words which occur in more than this many documents. Default is Int32.MaxValue.
     */
    public Integer getMaximumDocumentFrequency() {
        return maximumDocumentFrequency;
    }

    /**
     * @param maximumDocumentFrequency Ignore words which occur in more than this many documents. Default is Int32.MaxValue.
     */
    public void setMaximumDocumentFrequency(Integer maximumDocumentFrequency) {
        this.maximumDocumentFrequency = maximumDocumentFrequency;
    }

    /**
     * @return Ignore words which occur in more than this percentage of documents.
     */
    public Integer getMaximumDocumentFrequencyPercentage() {
        return maximumDocumentFrequencyPercentage;
    }

    /**
     * @param maximumDocumentFrequencyPercentage Ignore words which occur in more than this percentage of documents.
     */
    public void setMaximumDocumentFrequencyPercentage(Integer maximumDocumentFrequencyPercentage) {
        this.maximumDocumentFrequencyPercentage = maximumDocumentFrequencyPercentage;
    }

    /**
     * @return Boost terms in query based on score. Default is false.
     */
    public Boolean getBoost() {
        return boost;
    }

    /**
     * @param boost Boost terms in query based on score. Default is false.
     */
    public void setBoost(Boolean boost) {
        this.boost = boost;
    }

    /**
     * @return Boost factor when boosting based on score. Default is 1.
     */
    public Float getBoostFactor() {
        return boostFactor;
    }

    /**
     * @param boostFactor Boost factor when boosting based on score. Default is 1.
     */
    public void setBoostFactor(Float boostFactor) {
        this.boostFactor = boostFactor;
    }

    /**
     * @return The document id containing the custom stop words
     */
    public String getStopWordsDocumentId() {
        return stopWordsDocumentId;
    }

    /**
     * @param stopWordsDocumentId The document id containing the custom stop words
     */
    public void setStopWordsDocumentId(String stopWordsDocumentId) {
        this.stopWordsDocumentId = stopWordsDocumentId;
    }

    /**
     * @return The fields to compare
     */
    public String[] getFields() {
        return fields;
    }

    /**
     * @param fields The fields to compare
     */
    public void setFields(String[] fields) {
        this.fields = fields;
    }
}
