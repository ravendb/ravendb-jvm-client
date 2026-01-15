package net.ravendb.client.documents.operations.etl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;

import java.util.*;

public abstract class EtlConfiguration<T extends ConnectionString> {
    private long taskId;
    private String name;
    private String mentorNode;
    protected boolean initialized;
    private boolean pinToMentorNode;
    private String connectionStringName;
    private List<Transformation> transforms = new ArrayList<>();
    private boolean disabled;
    private boolean allowEtlOnNonEncryptedChannel;
    private T connection;
    private boolean testMode;

    public T getConnection() { return  this.connection; }

    public void initialize(T connectionString) {
        if (isInitialized()) {
            assert connectionString == connection :
                    "ConnectionString must be the same instance when reinitializing";
            return;
        }

        this.connection = connectionString;
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public void setConnection(T connection) { this. connection = connection; }

    public long getTaskId() {
        return taskId;
    }

    public void setTestMode(boolean value) { this.testMode = value; }
    public boolean isTestMode() { return this.testMode; }

    public boolean validate(
            List<String> errors,
            boolean validateName,
            boolean validateConnection,
            boolean validateIdentifier) {

        if (validateConnection && !isInitialized()) {
            throw new IllegalStateException("ETL configuration must be initialized");
        }

        errors.clear();

        if (validateName && (getName() == null || getName().isEmpty())) {
            errors.add("Name of ETL configuration cannot be empty");
        }

        if (!isTestMode() && (getConnectionStringName() == null || getConnectionStringName().isEmpty())) {
            errors.add("ConnectionStringName cannot be empty");
        }

        if (validateConnection && !isTestMode()) {
            getConnection().validate(errors);
        }

        Set<String> uniqueNames = new HashSet<>((Collection) String.CASE_INSENSITIVE_ORDER);

        if (getTransforms().isEmpty()) {
            throw new IllegalStateException("'Transforms' list cannot be empty.");
        }

        for (Transformation script : getTransforms()) {
            script.validate(errors, getEtlType());

            if (!uniqueNames.add(script.getName())) {
                errors.add("Script name '" + script.getName() +
                        "' name is already defined. The script names need to be unique");
            }
        }

        return errors.isEmpty();
    }


    @JsonIgnore
    public abstract String getDestination();

    public abstract String getDefaultTaskName();

    public abstract boolean usingEncryptedCommunicationChannel();

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    public boolean isPinToMentorNode() {
        return pinToMentorNode;
    }

    public void setPinToMentorNode(boolean pinToMentorNode) {
        this.pinToMentorNode = pinToMentorNode;
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public List<Transformation> getTransforms() {
        return transforms;
    }

    public void setTransforms(List<Transformation> transforms) {
        this.transforms = transforms;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isAllowEtlOnNonEncryptedChannel() {
        return allowEtlOnNonEncryptedChannel;
    }

    public void setAllowEtlOnNonEncryptedChannel(boolean allowEtlOnNonEncryptedChannel) {
        this.allowEtlOnNonEncryptedChannel = allowEtlOnNonEncryptedChannel;
    }

    public abstract EtlType getEtlType();
}
