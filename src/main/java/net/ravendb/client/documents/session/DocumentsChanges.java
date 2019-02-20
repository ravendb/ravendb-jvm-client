package net.ravendb.client.documents.session;


import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.apache.commons.lang3.StringUtils;

public class DocumentsChanges {

    private Object fieldOldValue;

    private Object fieldNewValue;

    private JsonNodeType fieldOldType;

    private JsonNodeType fieldNewType;

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

    /**
     * FieldOldType is not supported anymore. Will be removed in next major version of the product.
     * @return old field type
     */
    @Deprecated
    public JsonNodeType getFieldOldType() {
        return fieldOldType;
    }

    /**
     * FieldOldType is not supported anymore. Will be removed in next major version of the product.
     * @param fieldOldType old field type
     */
    @Deprecated
    public void setFieldOldType(JsonNodeType fieldOldType) {
        this.fieldOldType = fieldOldType;
    }

    /**
     * FieldNewType is not supported anymore. Will be removed in next major version of the product.
     * @return new field type
     */
    @Deprecated
    public JsonNodeType getFieldNewType() {
        return fieldNewType;
    }

    /**
     * FieldNewType is not supported anymore. Will be removed in next major version of the product.
     * @param fieldNewType new field type
     */
    @Deprecated
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
        ARRAY_VALUE_REMOVED,
        FIELD_TYPE_CHANGED,
        ENTITY_TYPE_CHANGED
    }
}
