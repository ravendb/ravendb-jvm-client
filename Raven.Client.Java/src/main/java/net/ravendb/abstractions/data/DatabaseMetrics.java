package net.ravendb.abstractions.data;

import java.util.HashMap;
import java.util.Map;


public class DatabaseMetrics {

  public static class OneMinuteMetricData implements IMetricsData {
    private int count;
    private long min;
    private long max;
    private double avg;

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public long getMin() {
      return min;
    }

    public void setMin(long min) {
      this.min = min;
    }

    public long getMax() {
      return max;
    }

    public void setMax(long max) {
      this.max = max;
    }

    public double getAvg() {
      return avg;
    }

    public void setAvg(double avg) {
      this.avg = avg;
    }
  }

  private double docsWritesPerSecond;
  private double indexedPerSecond;
  private double reducedPerSecond;
  private double requestsPerSecond;
  private MeterData requests;
  private HistogramData requestDuration;
  private OneMinuteMetricData requestDurationLastMinute;
  private HistogramData staleIndexMaps;
  private HistogramData staleIndexReduces;
  private Map<String, Map<String, String>> gauges;
  private Map<String, MeterData> replicationBatchSizeMeter;
  private Map<String, MeterData> replicationDurationMeter;
  private Map<String, HistogramData> replicationBatchSizeHistogram;
  private Map<String, HistogramData> replicationDurationHistogram;

  public HistogramData getStaleIndexMaps() {
    return staleIndexMaps;
  }


  public void setStaleIndexMaps(HistogramData staleIndexMaps) {
    this.staleIndexMaps = staleIndexMaps;
  }


  public HistogramData getStaleIndexReduces() {
    return staleIndexReduces;
  }


  public void setStaleIndexReduces(HistogramData staleIndexReduces) {
    this.staleIndexReduces = staleIndexReduces;
  }


  public Map<String, Map<String, String>> getGauges() {
    return gauges;
  }


  public void setGauges(Map<String, Map<String, String>> gauges) {
    this.gauges = gauges;
  }


  public Map<String, MeterData> getReplicationBatchSizeMeter() {
    return replicationBatchSizeMeter;
  }


  public void setReplicationBatchSizeMeter(Map<String, MeterData> replicationBatchSizeMeter) {
    this.replicationBatchSizeMeter = replicationBatchSizeMeter;
  }


  public Map<String, MeterData> getReplicationDurationMeter() {
    return replicationDurationMeter;
  }


  public OneMinuteMetricData getRequestDurationLastMinute() {
    return requestDurationLastMinute;
  }

  public void setRequestDurationLastMinute(OneMinuteMetricData requestDurationLastMinute) {
    this.requestDurationLastMinute = requestDurationLastMinute;
  }

  public void setReplicationDurationMeter(Map<String, MeterData> replicationDurationMeter) {
    this.replicationDurationMeter = replicationDurationMeter;
  }


  public Map<String, HistogramData> getReplicationBatchSizeHistogram() {
    return replicationBatchSizeHistogram;
  }


  public void setReplicationBatchSizeHistogram(Map<String, HistogramData> replicationBatchSizeHistogram) {
    this.replicationBatchSizeHistogram = replicationBatchSizeHistogram;
  }


  public Map<String, HistogramData> getReplicationDurationHistogram() {
    return replicationDurationHistogram;
  }


  public void setReplicationDurationHistogram(Map<String, HistogramData> replicationDurationHistogram) {
    this.replicationDurationHistogram = replicationDurationHistogram;
  }

  public double getDocsWritesPerSecond() {
    return docsWritesPerSecond;
  }

  public void setDocsWritesPerSecond(double docsWritesPerSecond) {
    this.docsWritesPerSecond = docsWritesPerSecond;
  }

  public double getIndexedPerSecond() {
    return indexedPerSecond;
  }

  public void setIndexedPerSecond(double indexedPerSecond) {
    this.indexedPerSecond = indexedPerSecond;
  }

  public double getReducedPerSecond() {
    return reducedPerSecond;
  }

  public void setReducedPerSecond(double reducedPerSecond) {
    this.reducedPerSecond = reducedPerSecond;
  }

  public double getRequestsPerSecond() {
    return requestsPerSecond;
  }

  public void setRequestsPerSecond(double requestsPerSecond) {
    this.requestsPerSecond = requestsPerSecond;
  }

  public MeterData getRequests() {
    return requests;
  }

  public void setRequests(MeterData requests) {
    this.requests = requests;
  }

  public HistogramData getRequestDuration() {
    return requestDuration;
  }

  public void setRequestDuration(HistogramData requestDuration) {
    this.requestDuration = requestDuration;
  }

  public static class HistogramData implements IMetricsData {
    private long counter;
    private double max;
    private double min;
    private double mean;
    private double stdev;

    public MetricType getMetricType() {
      return MetricType.HISTOGRAM;
    }

    private Map<String, Double> percentiles = new HashMap<>();

    public long getCounter() {
      return counter;
    }

    public void setCounter(long counter) {
      this.counter = counter;
    }

    public double getMax() {
      return max;
    }

    public void setMax(double max) {
      this.max = max;
    }

    public double getMin() {
      return min;
    }

    public void setMin(double min) {
      this.min = min;
    }

    public double getMean() {
      return mean;
    }

    public void setMean(double mean) {
      this.mean = mean;
    }

    public double getStdev() {
      return stdev;
    }

    public void setStdev(double stdev) {
      this.stdev = stdev;
    }

    public Map<String, Double> getPercentiles() {
      return percentiles;
    }

    public void setPercentiles(Map<String, Double> percentiles) {
      this.percentiles = percentiles;
    }
  }

  public static class MeterData implements IMetricsData {
    private long count;
    private double meanRate;
    private double oneMinuteRate;
    private double fiveMinuteRate;
    private double fiftennMinuteRate;

    public MetricType getMetricType() {
      return MetricType.METER;
    }

    public long getCount() {
      return count;
    }

    public void setCount(long count) {
      this.count = count;
    }

    public double getMeanRate() {
      return meanRate;
    }

    public void setMeanRate(double meanRate) {
      this.meanRate = meanRate;
    }

    public double getOneMinuteRate() {
      return oneMinuteRate;
    }

    public void setOneMinuteRate(double oneMinuteRate) {
      this.oneMinuteRate = oneMinuteRate;
    }

    public double getFiveMinuteRate() {
      return fiveMinuteRate;
    }

    public void setFiveMinuteRate(double fiveMinuteRate) {
      this.fiveMinuteRate = fiveMinuteRate;
    }

    public double getFiftennMinuteRate() {
      return fiftennMinuteRate;
    }

    public void setFiftennMinuteRate(double fiftennMinuteRate) {
      this.fiftennMinuteRate = fiftennMinuteRate;
    }

  }

}
