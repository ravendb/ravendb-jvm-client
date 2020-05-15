package net.ravendb.client.documents.smuggler;

public class DatabaseSmugglerImportOptions extends DatabaseSmugglerOptions implements IDatabaseSmugglerImportOptions {

    private boolean skipRevisionCreation;

    public DatabaseSmugglerImportOptions() {
    }

    public DatabaseSmugglerImportOptions(DatabaseSmugglerOptions options) {
        setIncludeExpired(options.isIncludeExpired());
        setIncludeArtificial(options.isIncludeArtificial());
        setMaxStepsForTransformScript(options.getMaxStepsForTransformScript());
        setOperateOnTypes(options.getOperateOnTypes().clone());
        setRemoveAnalyzers(options.isRemoveAnalyzers());
        setTransformScript(options.getTransformScript());
    }

    @Override
    public boolean isSkipRevisionCreation() {
        return skipRevisionCreation;
    }

    @Override
    public void setSkipRevisionCreation(boolean skipRevisionCreation) {
        this.skipRevisionCreation = skipRevisionCreation;
    }
}
