package net.ravendb.client.documents.session;

/**
 * Parameters for the Where Equals call
 */
public class WhereParams {

    private String fieldName;
    private Object value;
    private boolean allowWildcards;
    private boolean nestedPath;
    private boolean exact;

    public WhereParams() {
        nestedPath = false;
        allowWildcards = false;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isAllowWildcards() {
        return allowWildcards;
    }

    public void setAllowWildcards(boolean allowWildcards) {
        this.allowWildcards = allowWildcards;
    }

    public boolean isNestedPath() {
        return nestedPath;
    }

    public void setNestedPath(boolean nestedPath) {
        this.nestedPath = nestedPath;
    }

    public boolean isExact() {
        return exact;
    }

    public void setExact(boolean exact) {
        this.exact = exact;
    }

}
