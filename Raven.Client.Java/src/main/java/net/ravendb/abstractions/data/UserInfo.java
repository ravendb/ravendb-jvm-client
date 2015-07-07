package net.ravendb.abstractions.data;

import java.util.List;
import java.util.Set;

public class UserInfo {
    private String remark;
    private String user;
    private boolean adminGlobal;
    private boolean adminCurrentDb;
    private List<DatabaseInfo> databases;
    private Set<String> adminDatabases;
    private Set<String> readOnlyDatabases;
    private Set<String> readWriteDatabases;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isAdminGlobal() {
        return adminGlobal;
    }

    public void setAdminGlobal(boolean adminGlobal) {
        this.adminGlobal = adminGlobal;
    }

    public boolean isAdminCurrentDb() {
        return adminCurrentDb;
    }

    public void setAdminCurrentDb(boolean adminCurrentDb) {
        this.adminCurrentDb = adminCurrentDb;
    }

    public List<DatabaseInfo> getDatabases() {
        return databases;
    }

    public void setDatabases(List<DatabaseInfo> databases) {
        this.databases = databases;
    }

    public Set<String> getAdminDatabases() {
        return adminDatabases;
    }

    public void setAdminDatabases(Set<String> adminDatabases) {
        this.adminDatabases = adminDatabases;
    }

    public Set<String> getReadOnlyDatabases() {
        return readOnlyDatabases;
    }

    public void setReadOnlyDatabases(Set<String> readOnlyDatabases) {
        this.readOnlyDatabases = readOnlyDatabases;
    }

    public Set<String> getReadWriteDatabases() {
        return readWriteDatabases;
    }

    public void setReadWriteDatabases(Set<String> readWriteDatabases) {
        this.readWriteDatabases = readWriteDatabases;
    }
}
