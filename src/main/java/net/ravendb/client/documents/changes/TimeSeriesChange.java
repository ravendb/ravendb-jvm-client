package net.ravendb.client.documents.changes;

import java.util.Date;

public class TimeSeriesChange extends DatabaseChange {
    private String name;
    private Date from;
    private Date to;
    private String documentId;
    private String changeVector;
    private TimeSeriesChangeTypes type;
    private String collectionName;

    /**
     * @return Time Series name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Time Series name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Apply values of time series from date.
     */
    public Date getFrom() {
        return from;
    }

    /**
     * @param from Apply values of time series from date.
     */
    public void setFrom(Date from) {
        this.from = from;
    }

    /**
     * @return Apply values of time series to date.
     */
    public Date getTo() {
        return to;
    }

    /**
     * @param to Apply values of time series to date.
     */
    public void setTo(Date to) {
        this.to = to;
    }

    /**
     * @return Time series document identifier.
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * @param documentId Time series document identifier.
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * @return Time series change vector.
     */
    public String getChangeVector() {
        return changeVector;
    }

    /**
     * @param changeVector Time series change vector.
     */
    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    /**
     * @return Type of change that occurred on time series.
     */
    public TimeSeriesChangeTypes getType() {
        return type;
    }

    /**
     * @param type Type of change that occurred on time series.
     */
    public void setType(TimeSeriesChangeTypes type) {
        this.type = type;
    }

    /**
     * @return Collection name
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * @param collectionName Collection name
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}
