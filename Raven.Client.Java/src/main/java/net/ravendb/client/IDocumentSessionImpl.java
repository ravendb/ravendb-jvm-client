package net.ravendb.client;

import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.abstractions.basic.Tuple;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.batches.IEagerSessionOperations;

import java.util.Map;

/**
 * Interface for document session which holds the internal operations
 */
public interface IDocumentSessionImpl extends IDocumentSession, IEagerSessionOperations {

  DocumentConvention getConventions();

  <T> T[] loadInternal(Class<T> clazz, String[] ids);

  <T> T[] loadInternal(Class<T> clazz, String[] ids, Tuple<String, Class<?>>[] includes);

  <T> T[] loadInternal(Class<T> clazz, String[] ids, String transformer);

  <T> T[] loadInternal(Class<T> clazz, String[] ids, String transformer, Map<String, RavenJToken> transformerParameters);

  <T> T[] loadInternal(Class<T> clazz, String[] ids, Tuple<String, Class<?>>[] includes, String transformer);

  <T> T[] loadInternal(Class<T> clazz, String[] ids, Tuple<String, Class<?>>[] includes, String transformer, Map<String, RavenJToken> transformerParameters);

  <T> Lazy<T[]> lazyLoadInternal(Class<T> clazz, String[] ids, Tuple<String, Class<?>>[] includes, Action1<T[]> onEval);
}
