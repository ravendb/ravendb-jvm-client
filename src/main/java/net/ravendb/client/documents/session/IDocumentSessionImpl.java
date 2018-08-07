package net.ravendb.client.documents.session;

import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.operations.lazy.IEagerSessionOperations;

import java.util.Map;
import java.util.function.Consumer;

public interface IDocumentSessionImpl extends IDocumentSession, IEagerSessionOperations {

    DocumentConventions getConventions();

    <T> Map<String, T> loadInternal(Class<T> clazz, String[] ids, String[] includes);

    <T> Map<String, T> loadInternal(Class<T> clazz, String[] ids, String[] includes, String[] counterIncludes);

    <T> Map<String, T> loadInternal(Class<T> clazz, String[] ids, String[] includes, String[] counterIncludes, boolean includeAllCounters);

    <T> Lazy<Map<String, T>> lazyLoadInternal(Class<T> clazz, String[] ids, String[] includes, Consumer<Map<String, T>> onEval);
}
