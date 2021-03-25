package net.ravendb.client.documents.session;

import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.conventions.DocumentConventions;
import java.util.Map;
import java.util.function.Consumer;

public interface IDocumentSessionImpl extends IDocumentSession {

    DocumentConventions getConventions();

    <T> Map<String, T> loadInternal(Class<T> clazz, String[] ids, String[] includes);

    <T> Map<String, T> loadInternal(Class<T> clazz, String[] ids, String[] includes, String[] counterIncludes);

    <T> Map<String, T> loadInternal(Class<T> clazz, String[] ids, String[] includes, String[] counterIncludes,
                                    boolean includeAllCounters);

}
