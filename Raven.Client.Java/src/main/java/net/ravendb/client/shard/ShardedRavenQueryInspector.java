package net.ravendb.client.shard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ravendb.abstractions.closure.Function2;
import net.ravendb.abstractions.data.Facet;
import net.ravendb.abstractions.data.FacetResult;
import net.ravendb.abstractions.data.FacetResults;
import net.ravendb.abstractions.data.FacetValue;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.linq.RavenQueryInspector;
import net.ravendb.client.utils.Lang;


public class ShardedRavenQueryInspector<T> extends RavenQueryInspector<T> {

  private final ShardStrategy shardStrategy;
  private final List<IDatabaseCommands> shardDbCommands;

  public ShardedRavenQueryInspector(ShardStrategy shardStrategy,
    Collection<IDatabaseCommands> shardDbCommands) {
    this.shardStrategy = shardStrategy;
    this.shardDbCommands = new ArrayList<>(shardDbCommands);
  }

  @Override
  public FacetResults getFacets(final String facetSetupDoc, final int start, final Integer pageSize) {
    final IndexQuery indexQuery = getIndexQuery();

    ShardRequestData request = new ShardRequestData();
    request.setIndexName(getIndexQueried());
    request.setEntityType(getElementType());
    request.setQuery(indexQuery);

    FacetResults[] results = shardStrategy.getShardAccessStrategy().apply(FacetResults.class, shardDbCommands, request, new Function2<IDatabaseCommands, Integer, FacetResults>() {
      @Override
      public FacetResults apply(IDatabaseCommands commands, Integer i) {
        return commands.getFacets(getIndexQueried(), indexQuery, facetSetupDoc, start, pageSize);
      }
    });
    return mergeFacets(results);
  }

  @Override
  public FacetResults getFacets(final List<Facet> facets, final int start, final Integer pageSize) {
    final IndexQuery indexQuery = getIndexQuery();

    ShardRequestData shardRequestData = new ShardRequestData();
    shardRequestData.setIndexName(getIndexQueried());
    shardRequestData.setEntityType(getElementType());
    shardRequestData.setQuery(indexQuery);

    FacetResults[] results = shardStrategy.getShardAccessStrategy().apply(FacetResults.class, shardDbCommands, shardRequestData, new Function2<IDatabaseCommands, Integer, FacetResults>() {
      @Override
      public FacetResults apply(IDatabaseCommands commands, Integer i) {
        return commands.getFacets(getIndexQueried(), indexQuery, facets, start, pageSize);
      }
    });
    return mergeFacets(results);
  }

  @SuppressWarnings("boxing")
  private FacetResults mergeFacets(FacetResults[] results) {
    if (results == null) {
      return null;
    }
    if (results.length == 0) {
      return null;
    }
    if (results.length == 1) {
      return results[0];
    }

    FacetResults finalResult = new FacetResults();
    Map<FacetValue, List<Double>> avgs = new HashMap<>();

    for (FacetResults outerResult : results) {
      for (Map.Entry<String, FacetResult> result: outerResult.getResults().entrySet()) {
        FacetResult value = finalResult.getResults().get(result.getKey());
        if (value == null) {
          value = new FacetResult();
          finalResult.getResults().put(result.getKey(), value);
        }

        value.setRemainingHits(value.getRemainingHits() + result.getValue().getRemainingHits());
        if (result.getValue().getRemainingTerms() != null && result.getValue().getRemainingTerms().size() > 0) {
          Set<String> remainingTerms = new HashSet<>(result.getValue().getRemainingTerms());
          remainingTerms.addAll(value.getRemainingTerms());
          value.setRemainingTerms(new ArrayList<>(remainingTerms));
        }

        value.setRemainingHits(value.getRemainingHits() + result.getValue().getRemainingHits());

        for (FacetValue facetValue: result.getValue().getValues()) {
          FacetValue match = null;
          List<FacetValue> allValues = value.getValues();
          for (FacetValue vSearch: allValues) {
            if (vSearch.getRange().equals(facetValue.getRange())) {
              match = vSearch;
            }
          }
          if (match == null) {
            match = new FacetValue();
            match.setRange(facetValue.getRange());
            value.getValues().add(facetValue);
          }

          if (facetValue.getSum() != null) {
            match.setSum(match.getSum() + facetValue.getSum());
          }

          if (match.getMin() != null || facetValue.getMin() != null) {
            match.setMin(Math.min(Lang.coalesce(match.getMin(), Double.MAX_VALUE), Lang.coalesce(facetValue.getMin(), Double.MAX_VALUE)));
          }

          if (match.getMax() != null || facetValue.getMax() != null) {
            match.setMax(Math.min(Lang.coalesce(match.getMax(), Double.MIN_VALUE), Lang.coalesce(facetValue.getMax(), Double.MIN_VALUE)));
          }

          match.setHits(match.getHits() + facetValue.getHits());

          if (facetValue.getCount() != null) {
            match.setCount(match.getCount() + facetValue.getCount());
          }

          if (facetValue.getAverage() != null) {
            List<Double> list = avgs.get(match);
            if (list == null) {
              list = new ArrayList<>();
              avgs.put(match, list);
            }
            list.add(facetValue.getAverage());
          }

        }
      }
    }

    for (Map.Entry<FacetValue, List<Double>> avg : avgs.entrySet()) {
      avg.getKey().setAverage(calculateAverage(avg.getValue()));
    }
    return finalResult;
  }

  @SuppressWarnings({"boxing", "static-method"})
  private double calculateAverage(List<Double> items) {
    double sum = 0.0d;
    if(!items.isEmpty()) {
      for (Double mark : items) {
          sum += mark;
      }
      return sum / items.size();
    }
    return sum;
  }

}
