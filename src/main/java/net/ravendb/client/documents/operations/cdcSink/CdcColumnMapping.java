package net.ravendb.client.documents.operations.cdcSink;

/**
 * Maps a single SQL column to a RavenDB document property or attachment.
 */
public class CdcColumnMapping {

    private String column;
    private String name;
    private CdcColumnType type = CdcColumnType.DEFAULT;

    /**
     * @return the SQL column name in the source table
     */
    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    /**
     * @return the target name in RavenDB. For {@link CdcColumnType#DEFAULT} and {@link CdcColumnType#JSON}
     *         this is the document property name; for {@link CdcColumnType#ATTACHMENT} it is the attachment name.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return how this column is stored. {@link CdcColumnType#DEFAULT} stores it as a document property with
     *         standard type conversion, {@link CdcColumnType#JSON} parses the value as a native JSON object or
     *         array, and {@link CdcColumnType#ATTACHMENT} stores the raw value as a RavenDB attachment.
     */
    public CdcColumnType getType() {
        return type;
    }

    public void setType(CdcColumnType type) {
        this.type = type;
    }
}
