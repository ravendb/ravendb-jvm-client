package net.ravendb.client.documents.operations;

import net.ravendb.client.util.Size;

public class CollectionDetails {

    private String name;
    private long countOfDocuments;
    private long countOfTombstones;
    private long countOfRevisions;
    private long countOfTimeSeriesDeletedRanges;
    private long countOfCounterEntries;
    private long countOfTimeSeriesSegments;
    private Size timeSeriesSegmentsSize;
    private Size size;
    private Size documentsSize;
    private Size tombstonesSize;
    private Size revisionsSize;
    private Size timeSeriesDeletedRangesSize;
    private Size counterEntriesSize;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCountOfDocuments() {
        return countOfDocuments;
    }

    public void setCountOfDocuments(long countOfDocuments) {
        this.countOfDocuments = countOfDocuments;
    }

    public long getCountOfTombstones() {
        return countOfTombstones;
    }

    public void setCountOfTombstones(long countOfTombstones) {
        this.countOfTombstones = countOfTombstones;
    }

    public long getCountOfRevisions() {
        return countOfRevisions;
    }

    public void setCountOfRevisions(long countOfRevisions) {
        this.countOfRevisions = countOfRevisions;
    }

    public long getCountOfTimeSeriesDeletedRanges() {
        return countOfTimeSeriesDeletedRanges;
    }

    public void setCountOfTimeSeriesDeletedRanges(long countOfTimeSeriesDeletedRanges) {
        this.countOfTimeSeriesDeletedRanges = countOfTimeSeriesDeletedRanges;
    }

    public long getCountOfCounterEntries() {
        return countOfCounterEntries;
    }

    public void setCountOfCounterEntries(long countOfCounterEntries) {
        this.countOfCounterEntries = countOfCounterEntries;
    }

    public long getCountOfTimeSeriesSegments() {
        return countOfTimeSeriesSegments;
    }

    public void setCountOfTimeSeriesSegments(long countOfTimeSeriesSegments) {
        this.countOfTimeSeriesSegments = countOfTimeSeriesSegments;
    }

    public Size getTimeSeriesSegmentsSize() {
        return timeSeriesSegmentsSize;
    }

    public void setTimeSeriesSegmentsSize(Size timeSeriesSegmentsSize) {
        this.timeSeriesSegmentsSize = timeSeriesSegmentsSize;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public Size getDocumentsSize() {
        return documentsSize;
    }

    public void setDocumentsSize(Size documentsSize) {
        this.documentsSize = documentsSize;
    }

    public Size getTombstonesSize() {
        return tombstonesSize;
    }

    public void setTombstonesSize(Size tombstonesSize) {
        this.tombstonesSize = tombstonesSize;
    }

    public Size getRevisionsSize() {
        return revisionsSize;
    }

    public void setRevisionsSize(Size revisionsSize) {
        this.revisionsSize = revisionsSize;
    }

    public Size getTimeSeriesDeletedRangesSize() {
        return timeSeriesDeletedRangesSize;
    }

    public void setTimeSeriesDeletedRangesSize(Size timeSeriesDeletedRangesSize) {
        this.timeSeriesDeletedRangesSize = timeSeriesDeletedRangesSize;
    }

    public Size getCounterEntriesSize() {
        return counterEntriesSize;
    }

    public void setCounterEntriesSize(Size counterEntriesSize) {
        this.counterEntriesSize = counterEntriesSize;
    }
}
