package net.ravendb.client.documents.operations.cdcSink;

import java.util.ArrayList;
import java.util.List;

public class CdcSinkLinkedTableConfig {

    private String sourceTableSchema;
    private String sourceTableName;
    private String propertyName;
    private List<String> joinColumns = new ArrayList<>();
    private String linkedCollectionName;

    /**
     * @return the SQL schema name of the linked table
     */
    public String getSourceTableSchema() {
        return sourceTableSchema;
    }

    public void setSourceTableSchema(String sourceTableSchema) {
        this.sourceTableSchema = sourceTableSchema;
    }

    /**
     * @return the SQL table name of the linked table
     */
    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    /**
     * @return the property name in the document (e.g. "Customer")
     */
    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * @return the foreign key columns used to resolve the link
     */
    public List<String> getJoinColumns() {
        return joinColumns;
    }

    public void setJoinColumns(List<String> joinColumns) {
        this.joinColumns = joinColumns;
    }

    /**
     * @return the target collection name used for document ID generation
     *         (e.g. "Customers" generates "Customers/ALFKI")
     */
    public String getLinkedCollectionName() {
        return linkedCollectionName;
    }

    public void setLinkedCollectionName(String linkedCollectionName) {
        this.linkedCollectionName = linkedCollectionName;
    }
}
