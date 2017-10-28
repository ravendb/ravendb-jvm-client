package net.ravendb.client.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.collect.Lists;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.DocumentsChanges;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JsonOperation {

    public static boolean entityChanged(ObjectNode newObj, DocumentInfo documentInfo, Map<String, List<DocumentsChanges>> changes) {
        List<DocumentsChanges> docChanges = changes != null ? new ArrayList<>() : null;

        if (!documentInfo.isNewDocument() && documentInfo.getDocument() != null) {
            return compareJson(documentInfo.getId(), documentInfo.getDocument(), newObj, changes, docChanges);
        }

        if (changes == null) {
            return true;
        }

        newChange(null, null, null, docChanges, DocumentsChanges.ChangeType.DOCUMENT_ADDED);
        changes.put(documentInfo.getId(), docChanges);
        return true;
    }

    private static boolean compareJson(String id, ObjectNode originalJson, ObjectNode newJson, Map<String, List<DocumentsChanges>> changes, List<DocumentsChanges> docChanges) {
        ArrayList<String> newJsonProps = Lists.newArrayList(newJson.fieldNames());
        ArrayList<String> oldJsonProps = Lists.newArrayList(originalJson.fieldNames());

        Collection<String> newFields = CollectionUtils.subtract(newJsonProps, oldJsonProps);
        Collection<String> removedFields = CollectionUtils.subtract(oldJsonProps, newJsonProps);

        for (String field : removedFields) {
            if (changes == null) {
                return true;
            }
            newChange(field, null, null, docChanges, DocumentsChanges.ChangeType.REMOVED_FIELD);
        }

        for (String prop : newJsonProps) {

            if (Constants.Documents.Metadata.LAST_MODIFIED.equals(prop) ||
                    Constants.Documents.Metadata.COLLECTION.equals(prop) ||
                    Constants.Documents.Metadata.CHANGE_VECTOR.equals(prop) ||
                    Constants.Documents.Metadata.ID.equals(prop)) {
                continue;
            }

            if (newFields.contains(prop)) {
                if (changes == null) {
                    return true;
                }

                newChange(prop, newJson.get(prop), null, docChanges, DocumentsChanges.ChangeType.NEW_FIELD);
                continue;
            }

            JsonNode newProp = newJson.get(prop);
            JsonNode oldProp = originalJson.get(prop);

            switch (newProp.getNodeType()) {
                case NUMBER:
                case BOOLEAN:
                case STRING:
                    if (newProp.equals(oldProp) || compareValues((ValueNode) oldProp, (ValueNode) newProp)) {
                        break;
                    }
                    if (changes == null) {
                        return true;
                    }

                    newChange(prop, newProp, oldProp, docChanges, DocumentsChanges.ChangeType.FIELD_CHANGED);
                    break;
                case NULL:
                    if (oldProp.isNull()) {
                        break;
                    }
                    if (changes == null) {
                        return true;
                    }

                    newChange(prop, null, oldProp, docChanges, DocumentsChanges.ChangeType.FIELD_CHANGED);
                    break;
                case ARRAY:
                    if (oldProp == null || !(oldProp instanceof ArrayNode)) {
                        if (changes == null) {
                            return true;
                        }

                        newChange(prop, newProp, oldProp, docChanges, DocumentsChanges.ChangeType.FIELD_CHANGED);
                        break;
                    }

                    boolean changed = compareJsonArray(id, (ArrayNode) oldProp, (ArrayNode) newProp, changes, docChanges, prop);
                    if (changes == null && changed) {
                        return true;
                    }

                    break;
                case OBJECT:
                    if (oldProp == null || oldProp.isNull()) {
                        if (changes == null) {
                            return true;
                        }

                        newChange(prop, newProp, null, docChanges, DocumentsChanges.ChangeType.FIELD_CHANGED);
                        break;
                    }

                    changed = compareJson(id, (ObjectNode) oldProp, (ObjectNode) newProp, changes, docChanges);

                    if (changes == null && changed) {
                        return true;
                    }

                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }

        if (changes == null || docChanges.size() <= 0) {
            return false;
        }

        changes.put(id, docChanges);
        return true;
    }

    private static boolean compareValues(ValueNode oldProp, ValueNode newProp) {
        if (!oldProp.getNodeType().equals(newProp.getNodeType())) {
            return true;
        }

        return oldProp.asText().equals(newProp.asText());
    }

    private static boolean compareJsonArray(String id, ArrayNode oldArray, ArrayNode newArray, Map<String, List<DocumentsChanges>> changes, List<DocumentsChanges> docChanges, String propName) {
        // if we don't care about the changes
        if (oldArray.size() != newArray.size() && changes == null) {
            return true;
        }

        int position = 0;
        boolean changed = false;

        while (position < oldArray.size() && position < newArray.size()) {
            switch (oldArray.get(position).getNodeType()) {
                case OBJECT:
                    if (JsonNodeType.OBJECT.equals(newArray.get(position).getNodeType())) {
                        changed |= compareJson(id, (ObjectNode) oldArray.get(position), (ObjectNode) newArray.get(position), changes, docChanges);
                    } else {
                        changed = true;
                        if (changes != null) {
                            newChange(propName, newArray.get(position), oldArray.get(position), docChanges, DocumentsChanges.ChangeType.ARRAY_VALUE_ADDED);
                        }
                    }

                    break;
                case ARRAY:
                    if (JsonNodeType.ARRAY.equals(newArray.get(position).getNodeType())) {
                        changed |= compareJsonArray(id, (ArrayNode) oldArray.get(position), (ArrayNode) newArray.get(position), changes, docChanges, propName);
                    } else {
                        changed = true;
                        if (changes != null) {
                            newChange(propName, newArray.get(position), oldArray.get(position), docChanges, DocumentsChanges.ChangeType.ARRAY_VALUE_CHANGED);
                        }
                    }
                    break;
                case NULL:
                    if (newArray.get(position) != null && !newArray.get(position).isNull()) {
                        changed = true;
                        if (changes != null) {
                            newChange(propName, newArray.get(position), oldArray.get(position), docChanges, DocumentsChanges.ChangeType.ARRAY_VALUE_ADDED);
                        }
                    }
                    break;

                default:
                    if (!oldArray.get(position).asText().equals(newArray.get(position).asText())) {
                        if (changes != null) {
                            newChange(propName, newArray.get(position), oldArray.get(position), docChanges, DocumentsChanges.ChangeType.ARRAY_VALUE_CHANGED);
                        }
                        changed = true;
                    }
            }

            position++;
        }

        if (changes == null) {
            return changed;
        }

        // if one of the arrays is larger than the other
        while (position < oldArray.size()) {
            newChange(propName, null, oldArray.get(position), docChanges, DocumentsChanges.ChangeType.ARRAY_VALUE_REMOVED);
            position++;
        }

        while (position < newArray.size()) {
            newChange(propName, newArray.get(position), null, docChanges, DocumentsChanges.ChangeType.ARRAY_VALUE_ADDED);
            position++;
        }

        return changed;
    }

    private static void newChange(String name, Object newValue, Object oldValue, List<DocumentsChanges> docChanges, DocumentsChanges.ChangeType change) {
        if (newValue instanceof NumericNode) {
            NumericNode node = (NumericNode) newValue;
            newValue = node.numberValue();
        }

        if (oldValue instanceof NumericNode) {
            NumericNode node = (NumericNode) oldValue;
            oldValue = node.numberValue();
        }


        DocumentsChanges documentsChanges = new DocumentsChanges();
        documentsChanges.setFieldName(name);
        documentsChanges.setFieldNewValue(newValue);
        documentsChanges.setFieldOldValue(oldValue);
        documentsChanges.setChange(change);
        docChanges.add(documentsChanges);
    }
}
