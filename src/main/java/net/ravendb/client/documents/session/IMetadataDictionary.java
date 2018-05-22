package net.ravendb.client.documents.session;

import java.util.Map;

public interface IMetadataDictionary extends Map<String, Object> {

    boolean isDirty();

    IMetadataDictionary[] getObjects(String key);
}
