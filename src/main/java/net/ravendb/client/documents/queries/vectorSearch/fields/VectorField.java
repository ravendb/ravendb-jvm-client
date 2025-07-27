package net.ravendb.client.documents.queries.vectorSearch.fields;

import net.ravendb.client.documents.queries.vectorSearch.common.VectorFieldBase;
import net.ravendb.client.documents.session.IVectorField;

/**
 * Vector field implementation
 * @param <T> The type of the field
 */
public class VectorField<T> extends VectorFieldBase<T> implements IVectorField {
    /**
     * Creates a new instance of VectorField
     * @param fieldName The field name
     */
    public VectorField(T fieldName) {
        super(fieldName);
        this.byFieldMethodUsed = true;
        setFieldName(fieldName.toString());
    }
}