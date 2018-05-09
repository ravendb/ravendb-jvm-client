package net.ravendb.client.documents.queries.facets;

import org.apache.commons.lang3.StringUtils;

public class FacetValue {

    private String range;
    private int count;
    private Double sum;
    private Double max;
    private Double min;
    private Double average;

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Double getSum() {
        return sum;
    }

    public void setSum(Double sum) {
        this.sum = sum;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getAverage() {
        return average;
    }

    public void setAverage(Double average) {
        this.average = average;
    }

    @Override
    public String toString() {
        String msg = range + " - Count: " + count + ", ";
        if (sum != null) {
            msg += "Sum: " + sum + ",";
        }
        if (max != null) {
            msg += "Max: " + max + ",";
        }
        if (min != null) {
            msg += "Min: " + min + ",";
        }
        if (average != null) {
            msg += "Average: " + average + ",";
        }

        return StringUtils.removeEnd(msg, ";");
    }
}
