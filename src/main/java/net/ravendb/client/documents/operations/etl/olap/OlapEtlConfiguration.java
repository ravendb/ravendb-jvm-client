package net.ravendb.client.documents.operations.etl.olap;

import net.ravendb.client.documents.operations.backups.FtpSettings;
import net.ravendb.client.documents.operations.etl.EtlConfiguration;
import net.ravendb.client.documents.operations.etl.EtlType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class OlapEtlConfiguration extends EtlConfiguration<OlapConnectionString> {
    private String runFrequency;
    private OlapEtlFileFormat format;
    private String customPartitionValue;
    private List<OlapEtlTable> olapTables;

    private String name;
    private static final String SFTP = "sftp";
    private static final String FTPS = "ftps";

    @Override
    public String getDestination() {
        String name = this.getName();
        if (name == null) {
            name = (this.getConnection() != null) ? this.getConnection().getDestination() : null;
        }
        return name;
    }

    public void initialize(OlapConnectionString connectionString) {
        this.setConnection(connectionString);
        this.setInitialized(true);
    }

    @Override
    public String getDefaultTaskName() {
        return "OLAP ETL to " + this.getConnectionStringName();
    }

    @Override
    public boolean usingEncryptedCommunicationChannel() {
        FtpSettings settings = this.getConnection().getFtpSettings();
        if (settings == null) {
            return true;
        }

        String url = settings.getUrl();
        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return false; // cannot parse → treat as unencrypted
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }

        return scheme.equalsIgnoreCase(SFTP) ||
                scheme.equalsIgnoreCase(FTPS);
    }

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
