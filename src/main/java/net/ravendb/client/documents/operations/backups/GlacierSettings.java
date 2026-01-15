package net.ravendb.client.documents.operations.backups;

public class GlacierSettings extends AmazonSettings {
    private String vaultName;

    public String getVaultName() {
        return vaultName;
    }

    public void setVaultName(String vaultName) {
        this.vaultName = vaultName;
    }

    @Override
    public boolean hasSettings() {
        if (super.hasSettings()) {
            return true;
        }

        return vaultName != null && !vaultName.trim().isEmpty();
    }
}
