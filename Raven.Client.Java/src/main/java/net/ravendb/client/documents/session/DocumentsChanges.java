package net.ravendb.client.documents.session;


import com.fasterxml.jackson.databind.node.JsonNodeType;

public class DocumentsChanges {

    private Object fieldOldValue;

    private Object fieldNewValue;

    private JsonNodeType fieldOldType;

    private JsonNodeType fieldNewType;

    private ChangeType change;

    private String fieldName;

    public Object getFieldOldValue() {
        return fieldOldValue;
    }

    public void setFieldOldValue(Object fieldOldValue) {
        this.fieldOldValue = fieldOldValue;
    }

    public Object getFieldNewValue() {
        return fieldNewValue;
    }

    public void setFieldNewValue(Object fieldNewValue) {
        this.fieldNewValue = fieldNewValue;
    }

    public JsonNodeType getFieldOldType() {
        return fieldOldType;
    }

    public void setFieldOldType(JsonNodeType fieldOldType) {
        this.fieldOldType = fieldOldType;
    }

    public JsonNodeType getFieldNewType() {
        return fieldNewType;
    }

    public void setFieldNewType(JsonNodeType fieldNewType) {
        this.fieldNewType = fieldNewType;
    }

    public ChangeType getChange() {
        return change;
    }

    public void setChange(ChangeType change) {
        this.change = change;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DocumentsChanges that = (DocumentsChanges) o;

        if (fieldOldValue != null ? !fieldOldValue.equals(that.fieldOldValue) : that.fieldOldValue != null)
            return false;
        if (fieldNewValue != null ? !fieldNewValue.equals(that.fieldNewValue) : that.fieldNewValue != null)
            return false;
        if (fieldOldType != that.fieldOldType) return false;
        if (fieldNewType != that.fieldNewType) return false;
        if (change != that.change) return false;
        return fieldName != null ? fieldName.equals(that.fieldName) : that.fieldName == null;
    }

    @Override
    public int hashCode() {
        int result = fieldOldValue != null ? fieldOldValue.hashCode() : 0;
        result = 31 * result + (fieldNewValue != null ? fieldNewValue.hashCode() : 0);
        result = 31 * result + (fieldOldType != null ? fieldOldType.hashCode() : 0);
        result = 31 * result + (fieldNewType != null ? fieldNewType.hashCode() : 0);
        result = 31 * result + (change != null ? change.hashCode() : 0);
        result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
        return result;
    }

    public enum ChangeType {
        DOCUMENT_DELETED,
        DOCUMENT_ADDED,
        FIELD_CHANGED,
        NEW_FIELD,
        REMOVED_FIELD,
        ARRAY_VALUE_CHANGED,
        ARRAY_VALUE_ADDED,
        ARRAY_VALUE_REMOVED,
        FIELD_TYPE_CHANGED,
        ENTITY_TYPE_CHANGED
    }
}
