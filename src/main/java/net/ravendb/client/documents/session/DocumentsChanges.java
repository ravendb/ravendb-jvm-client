package net.ravendb.client.documents.session;


import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.apache.commons.lang3.StringUtils;

public class DocumentsChanges {

    private Object fieldOldValue;

    private Object fieldNewValue;

    private ChangeType change;

    private String fieldName;

    private String fieldPath;

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

    public String getFieldFullName() {
        return StringUtils.isEmpty(fieldPath) ? fieldName : fieldPath + "." + fieldName;
    }

    /**
     * @return Path of field on which the change occurred.
     */
    public String getFieldPath() {
        return fieldPath;
    }

    /**
     * @param fieldPath Path of field on which the change occurred.
     */
    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    public enum ChangeType {
        DOCUMENT_DELETED,
        DOCUMENT_ADDED,
        FIELD_CHANGED,
        NEW_FIELD,
        REMOVED_FIELD,
        ARRAY_VALUE_CHANGED,
        ARRAY_VALUE_ADDED,
        ARRAY_VALUE_REMOVED
    }
}
