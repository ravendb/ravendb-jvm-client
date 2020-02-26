package net.ravendb.client.documents.session;

import java.util.Map;

public interface IMetadataDictionary extends Map<String, Object> {

    boolean isDirty();

    IMetadataDictionary[] getObjects(String key);

    String getString(String key);

    long getLong(String key);

    boolean getBoolean(String key);

    double getDouble(String key);

    IMetadataDictionary getObject(String key);
}
