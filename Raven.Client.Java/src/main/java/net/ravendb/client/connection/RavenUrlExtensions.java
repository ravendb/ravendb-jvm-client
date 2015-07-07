package net.ravendb.client.connection;

import net.ravendb.client.extensions.MultiDatabase;
import net.ravendb.client.utils.UrlUtils;

import org.apache.commons.lang.StringUtils;


public class RavenUrlExtensions {

  public static String forDatabase(String url, String database) {
    if (StringUtils.isNotEmpty(database) && !url.contains("/databases/")){
      if (url.endsWith("/")) {
        return url + "databases/" + database;
      }
      return url + "/databases/" + database;
    }
    return url;
  }

  public static String indexes(String url, String index) {
    return url + "/indexes/" + index;
  }

  public static String indexDefinition(String url, String index) {
    return url + "/indexes/" + index + "?definition=yes";
  }

  public static String indexingPerformanceStatistics(String url) {
    return url + "/debug/indexing-perf-stats";
  }

  public static String tranformer(String url, String transformer) {
    return url + "/transformers/" + transformer;
  }

  public static String indexNames(String url, int start, int pageSize) {
    return url + "/indexes/?namesOnly=true&start=" + start + "&pageSize=" + pageSize;
  }

  public static String stats(String url) {
    return url + "/stats";
  }

  public static String userInfo(String url) {
    return url + "/debug/user-info";
  }

  public static String userPermission(String url, String database, boolean readOnly) {
    return url + "/debug/user-info" + "?database=" + database + "&method=" + (readOnly?"GET":"POST");
  }

  public static String adminStats(String url) {
    return MultiDatabase.getRootDatabaseUrl(url) + "/admin/stats";
  }

  public static String replicationInfo(String url) {
    return url + "/replication/info";
  }

  public static String lastReplicatedEtagFor(String destinationUrl, String sourceUrl, String sourceDbId) {
    return lastReplicatedEtagFor(destinationUrl, sourceUrl, sourceDbId, null);
  }

  public static String lastReplicatedEtagFor(String destinationUrl, String sourceUrl, String sourceDbId, String[] sourceCollections) {
    return destinationUrl + "/replication/lastEtag?from=" + UrlUtils.escapeDataString(sourceUrl)  + "&dbId=" + sourceDbId;
  }

  public static String databases(String url, int pageSize, int start) {
    String databases = MultiDatabase.getRootDatabaseUrl(url) + "/databases?pageSize=" + pageSize;
    return start > 0 ? databases + "&start=" + start : databases;
  }

  public static String terms(String url, String index, String field, String fromValue, int pageSize) {
    return url + "/terms/" + index + "?field=" + field + "&fromValue=" + fromValue + "&pageSize=" + pageSize;
  }

  public static String doc(String url, String key) {
    return url + "/docs/" + key;
  }

  public static String docs(String url, int start, int pageSize) {
    return url + "/docs/?start=" + start + "&pageSize=" + pageSize;
  }

  public static String queries(String url) {
    return url + "/queries/";
  }
}
