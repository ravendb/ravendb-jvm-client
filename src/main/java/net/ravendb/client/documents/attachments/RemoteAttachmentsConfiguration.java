package net.ravendb.client.documents.attachments;

import java.util.*;

/**
 * Configuration for remote attachments functionality, including destinations, frequency, and upload settings.
 */
public final class RemoteAttachmentsConfiguration {
    static final Comparator<String> KEY_COMPARER =
            Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);

    /**
     * Dictionary of remote attachment destinations, keyed by destination name (case‑insensitive).
     */
    private Map<String, RemoteAttachmentsDestinationConfiguration> destinations =
            new TreeMap<>(KEY_COMPARER);

    /**
     * Frequency (in seconds) at which the remote attachments process checks for new items to upload.
     */
    private Long checkFrequencyInSec;

    /**
     * Maximum number of items to process in a single batch.
     */
    private Long maxItemsToProcess;

    /**
     * Number of concurrent uploads allowed.
     */
    private Integer concurrentUploads;

    /**
     * Indicates whether remote attachments functionality is disabled.
     */
    private boolean disabled;

    public Map<String, RemoteAttachmentsDestinationConfiguration> getDestinations() {
        return destinations;
    }

    public void setDestinations(Map<String, RemoteAttachmentsDestinationConfiguration> destinations) {
        this.destinations = destinations;
    }

    public Long getCheckFrequencyInSec() {
        return checkFrequencyInSec;
    }

    public void setCheckFrequencyInSec(Long checkFrequencyInSec) {
        this.checkFrequencyInSec = checkFrequencyInSec;
    }

    public Long getMaxItemsToProcess() {
        return maxItemsToProcess;
    }

    public void setMaxItemsToProcess(Long maxItemsToProcess) {
        this.maxItemsToProcess = maxItemsToProcess;
    }

    public Integer getConcurrentUploads() {
        return concurrentUploads;
    }

    public void setConcurrentUploads(Integer concurrentUploads) {
        this.concurrentUploads = concurrentUploads;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Validates the configuration.
     */
    public void assertConfiguration(){
        assertConfiguration(null);
    }

    /**
     * Validates the configuration.
     */
    void assertConfiguration(String databaseName) {
        String suffix = (databaseName == null || databaseName.isEmpty())
                ? ""
                : " for database '" + databaseName + "'";

        if (checkFrequencyInSec != null && checkFrequencyInSec <= 0) {
            throw new IllegalStateException("Remote attachments check frequency" + suffix + " must be greater than 0.");
        }

        if (maxItemsToProcess != null && maxItemsToProcess <= 0) {
            throw new IllegalStateException("Max items to process" + suffix + " must be greater than 0.");
        }

        if (concurrentUploads != null && concurrentUploads <= 0) {
            throw new IllegalStateException("Concurrent attachments uploads" + suffix + " must be greater than 0.");
        }

        if (destinations == null || destinations.isEmpty()) {
            return;
        }

        Set<String> keys = new LinkedHashSet<>();

        for (Map.Entry<String, RemoteAttachmentsDestinationConfiguration> entry : destinations.entrySet()) {
            String key = entry.getKey();
            String normalizedKey;
            if (key != null)
                normalizedKey = key.toLowerCase(Locale.ROOT);
            else
                throw new IllegalArgumentException("Value cannot be null. (Parameter 'key')");
            RemoteAttachmentsDestinationConfiguration value = entry.getValue();

            if (!keys.add(normalizedKey)) {
                throw new IllegalStateException("Destination key '" + key + "' is duplicate. Duplicate keys are not allowed in remote attachments configuration" + suffix + ".");
            }

            if (value == null) {
                throw new IllegalStateException("Destination configuration for key " + key + " is null" + suffix + ".");
            }

            value.assertConfiguration(key, databaseName);
        }
    }
}
