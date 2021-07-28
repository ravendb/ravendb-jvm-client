package net.ravendb.client.documents.session.timeSeries;

import net.ravendb.client.exceptions.RavenException;

import java.util.Date;

public class TypedTimeSeriesRollupEntry<TValues> {
    private Class<TValues> _clazz;

    private Date _timestamp;
    private String _tag;
    private boolean _rollup;

    private TValues _first;
    private TValues _last;
    private TValues _max;
    private TValues _min;
    private TValues _sum;
    private TValues _count;

    private TValues _average;

    public TypedTimeSeriesRollupEntry(Class<TValues> clazz, Date timestamp) {
        _clazz = clazz;
        _rollup = true;
        _timestamp = timestamp;
    }

    public Date getTimestamp() {
        return _timestamp;
    }

    public void setTimestamp(Date timestamp) {
        _timestamp = timestamp;
    }

    public String getTag() {
        return _tag;
    }

    public void setTag(String tag) {
        _tag = tag;
    }

    public boolean isRollup() {
        return _rollup;
    }

    public void setRollup(boolean rollup) {
        _rollup = rollup;
    }

    @SuppressWarnings("deprecation")
    private TValues createInstance() {
        try {
            return _clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RavenException("Unable to create instance of class: " + _clazz.getSimpleName(), e);
        }
    }

    public TValues getMax() {
        if (_max == null) {
            _max = createInstance();
        }

        return _max;
    }

    public TValues getMin() {
        if (_min == null) {
            _min = createInstance();
        }
        return _min;
    }

    public TValues getCount() {
        if (_count == null) {
            _count = createInstance();
        }

        return _count;
    }

    public TValues getFirst() {
        if (_first == null) {
            _first = createInstance();
        }

        return _first;
    }

    public TValues getLast() {
        if (_last == null) {
            _last = createInstance();
        }

        return _last;
    }

    public TValues getSum() {
        if (_sum == null) {
            _sum = createInstance();
        }

        return _sum;
    }

    public TValues getAverage() {
        if (_average != null) {
            return _average;
        }

        int valuesCount = TimeSeriesValuesHelper.getFieldsMapping(_clazz).size();

        double[] sums = TimeSeriesValuesHelper.getValues(_clazz, _sum);
        double[] counts = TimeSeriesValuesHelper.getValues(_clazz, _count);
        double[] averages = new double[valuesCount];

        for (int i = 0; i < valuesCount; i++) {
            if (counts[i] < Double.MIN_NORMAL) {
                averages[i] = Double.NaN;
            } else {
                averages[i] = sums[i] / counts[i];
            }
        }

        _average = TimeSeriesValuesHelper.setFields(_clazz, averages);

        return _average;
    }

    public double[] getValuesFromMembers() {
        int valuesCount = TimeSeriesValuesHelper.getFieldsMapping(_clazz).size();

        double[] result = new double[valuesCount * 6];
        assignRollup(result, _first, 0);
        assignRollup(result, _last, 1);
        assignRollup(result, _min, 2);
        assignRollup(result, _max, 3);
        assignRollup(result, _sum, 4);
        assignRollup(result, _count, 5);

        return result;
    }

    private void assignRollup(double[] target, TValues source, int offset) {
        if (source != null) {
            double[] values = TimeSeriesValuesHelper.getValues(_clazz, source);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    target[i * 6 + offset] = values[i];
                }
            }
        }
    }

    public static <T> TypedTimeSeriesRollupEntry<T> fromEntry(Class<T> clazz, TimeSeriesEntry entry) {
        TypedTimeSeriesRollupEntry<T> result = new TypedTimeSeriesRollupEntry<>(clazz, entry.getTimestamp());
        result.setRollup(true);
        result.setTag(entry.getTag());

        double[] values = entry.getValues();

        result._first = TimeSeriesValuesHelper.setFields(clazz, extractValues(values, 0));
        result._last = TimeSeriesValuesHelper.setFields(clazz, extractValues(values, 1));
        result._min = TimeSeriesValuesHelper.setFields(clazz, extractValues(values, 2));
        result._max = TimeSeriesValuesHelper.setFields(clazz, extractValues(values, 3));
        result._sum = TimeSeriesValuesHelper.setFields(clazz, extractValues(values, 4));
        result._count = TimeSeriesValuesHelper.setFields(clazz, extractValues(values, 5));

        return result;
    }

    private static double[] extractValues(double[] input, int offset) {
        int length = (int) Math.ceil((input.length - offset) / 6.0);
        int idx = 0;
        double[] result = new double[length];

        while (idx < length) {
            result[idx] = input[offset + idx * 6];
            idx++;
        }

        return result;
    }
}
