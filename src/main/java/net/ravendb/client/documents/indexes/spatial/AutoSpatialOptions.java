package net.ravendb.client.documents.indexes.spatial;

import java.util.List;

public class AutoSpatialOptions extends SpatialOptions {
    private AutoSpatialMethodType methodType;
    private List<String> methodArguments;

    public AutoSpatialMethodType getMethodType() {
        return methodType;
    }

    public void setMethodType(AutoSpatialMethodType methodType) {
        this.methodType = methodType;
    }

    public List<String> getMethodArguments() {
        return methodArguments;
    }

    public void setMethodArguments(List<String> methodArguments) {
        this.methodArguments = methodArguments;
    }
}
