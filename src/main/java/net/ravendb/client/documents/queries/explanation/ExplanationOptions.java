package net.ravendb.client.documents.queries.explanation;

/**
 * Additional configuration to explanation query.
 */
public class ExplanationOptions {
    private String groupKey;

    /**
     * Scope explanation to specific group by key.
     * @return Group key
     */
    public String getGroupKey() {
        return groupKey;
    }

    /**
     * Scope explanation to specific group by key.
     * @param groupKey Group key
     */
    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }
}
