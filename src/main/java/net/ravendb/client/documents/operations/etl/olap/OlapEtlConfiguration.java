package net.ravendb.client.documents.operations.etl.olap;

import net.ravendb.client.documents.operations.etl.EtlConfiguration;
import net.ravendb.client.documents.operations.etl.EtlType;

import java.util.List;

public class OlapEtlConfiguration extends EtlConfiguration<OlapConnectionString> {
    private String runFrequency;
    private OlapEtlFileFormat format;
    private String customPartitionValue;
    private List<OlapEtlTable> olapTables;

    @Override
    public EtlType getEtlType() {
        return EtlType.OLAP;
    }

    public String getRunFrequency() {
        return runFrequency;
    }

    public void setRunFrequency(String runFrequency) {
        this.runFrequency = runFrequency;
    }

    public OlapEtlFileFormat getFormat() {
        return format;
    }

    public void setFormat(OlapEtlFileFormat format) {
        this.format = format;
    }

    public String getCustomPartitionValue() {
        return customPartitionValue;
    }

    public void setCustomPartitionValue(String customPartitionValue) {
        this.customPartitionValue = customPartitionValue;
    }

    public List<OlapEtlTable> getOlapTables() {
        return olapTables;
    }

    public void setOlapTables(List<OlapEtlTable> olapTables) {
        this.olapTables = olapTables;
    }
}
