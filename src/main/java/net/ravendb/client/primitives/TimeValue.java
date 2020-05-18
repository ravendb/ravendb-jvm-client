package net.ravendb.client.primitives;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class TimeValue implements Comparable<TimeValue> {

    private final static int SECONDS_PER_DAY = 86_400;
    private final static int SECONDS_IN_28_DAYS = 28 * SECONDS_PER_DAY; // lower-bound of seconds in month
    private final static int SECONDS_IN_31_DAYS = 31 * SECONDS_PER_DAY; // upper-bound of seconds in month
    private final static int SECONDS_IN_365_DAYS = 365 * SECONDS_PER_DAY; // lower-bound of seconds in a year
    private final static int SECONDS_IN_366_DAYS = 366 * SECONDS_PER_DAY; // upper-bound of seconds in a year

    public static final TimeValue ZERO = new TimeValue();

    public static final TimeValue MAX_VALUE = new TimeValue();

    public static final TimeValue MIN_VALUE = new TimeValue();

    static {
        ZERO._value = 0;
        MAX_VALUE._value = Integer.MAX_VALUE;
        MIN_VALUE._value = Integer.MIN_VALUE;
    }

    private int _value;
    private TimeValueUnit _unit;

    public int getValue() {
        return _value;
    }

    public TimeValueUnit getUnit() {
        return _unit;
    }

    private TimeValue() {

    }

    @JsonCreator
    private TimeValue(@JsonProperty("Value") int value, @JsonProperty("Unit") TimeValueUnit unit) {
        _value = value;
        _unit = unit;
    }

    public static TimeValue ofSeconds(int seconds) {
        return new TimeValue(seconds, TimeValueUnit.SECOND);
    }

    public static TimeValue ofMinutes(int minutes) {
        return new TimeValue(minutes * 60, TimeValueUnit.SECOND);
    }

    public static TimeValue ofHours(int hours) {
        return new TimeValue(hours * 3600, TimeValueUnit.SECOND);
    }

    public static TimeValue ofDays(int days) {
        return new TimeValue(days * SECONDS_PER_DAY, TimeValueUnit.SECOND);
    }

    public static TimeValue ofMonths(int months) {
        return new TimeValue(months, TimeValueUnit.MONTH);
    }

    public static TimeValue ofYears(int years) {
        return new TimeValue(12 * years, TimeValueUnit.MONTH);
    }

    private void append(StringBuilder builder, int value, String singular) {
        if (value <= 0) {
            return;
        }

        builder
                .append(value)
                .append(" ")
                .append(singular);

        if (value == 1) {
            builder
                    .append(" ");
            return;
        }

        builder.append("s "); // lucky me, no special rules here
    }

    @Override
    public String toString() {
        if (_value == Integer.MAX_VALUE) {
            return "MaxValue";
        }
        if (_value == Integer.MIN_VALUE) {
            return "MinValue";
        }

        if (_value == 0) {
            return "Zero";
        }

        if (_unit == TimeValueUnit.NONE) {
            return "Unknown time unit";
        }

        StringBuilder str = new StringBuilder();
        switch (_unit) {
            case SECOND:
                int remainingSeconds = _value;

                if (remainingSeconds > SECONDS_PER_DAY) {
                    int days = _value / SECONDS_PER_DAY;
                    append(str, days, "day");
                    remainingSeconds -= days * SECONDS_PER_DAY;
                }

                if (remainingSeconds > 3_600) {
                    int hours = remainingSeconds / 3_600;
                    append(str, hours, "hours");
                    remainingSeconds -= hours * 3_600;
                }

                if (remainingSeconds > 60) {
                    int minutes = remainingSeconds / 60;
                    append(str, minutes, "minute");
                    remainingSeconds -= minutes * 60;
                }

                if (remainingSeconds > 0) {
                    append(str, remainingSeconds, "second");
                }
                break;
            case MONTH:
                if (_value >= 12) {
                    append(str, _value / 12, "year");
                }
                if (_value % 12 > 0) {
                    append(str, _value % 12, "month");
                }
                break;

            default:
                throw new IllegalArgumentException("Not supported unit: " + _unit);
        }

        return str.toString().trim();
    }

    private void assertSeconds() {
        if (_unit != TimeValueUnit.SECOND) {
            throw new IllegalArgumentException("The value must be seconds");
        }
    }

    private static void assertValidUnit(TimeValueUnit unit) {
        if (unit == TimeValueUnit.MONTH || unit == TimeValueUnit.SECOND) {
            return;
        }

        throw new IllegalArgumentException("Invalid time unit: " + unit);
    }

    private static void assertSameUnits(TimeValue a , TimeValue b) {
        if (a.getUnit() != b.getUnit()) {
            throw new IllegalStateException("Unit isn't the same " + a.getUnit() + " != " + b.getUnit());
        }
    }

    @Override
    public int compareTo(TimeValue other) {
        if (_value == 0 || other._value == 0) {
            return _value - other._value;
        }

        Reference<Integer> resultRef = new Reference<>();
        if (isSpecialCompare(this, other, resultRef)) {
            return resultRef.value;
        }

        if (_unit == other._unit) {
            return trimCompareResult(_value - other._value);
        }

        Tuple<Long, Long> myBounds = getBoundsInSeconds(this);
        Tuple<Long, Long> otherBounds = getBoundsInSeconds(other);

        if (otherBounds.second < myBounds.first) {
            return 1;
        }

        if (otherBounds.first > myBounds.second) {
            return -1;
        }

        throw new IllegalStateException("Unable to compare " + this + " with " + other + ", since a month might have different number of days.");
    }

    private static Tuple<Long, Long> getBoundsInSeconds(TimeValue time) {
        switch (time._unit) {
            case SECOND:
                return Tuple.create((long) time._value, (long) time._value);
            case MONTH:
                int years = time._value / 12;
                long upperBound = years * SECONDS_IN_366_DAYS;
                long lowerBound = years * SECONDS_IN_365_DAYS;

                int remainingMonths = time._value % 12;
                upperBound += remainingMonths * SECONDS_IN_31_DAYS;
                lowerBound += remainingMonths * SECONDS_IN_28_DAYS;

                return Tuple.create(lowerBound, upperBound);
            default:
                throw new IllegalArgumentException("Not supported time value unit: " + time._unit);
        }
    }

    private static boolean isSpecialCompare(TimeValue current, TimeValue other, Reference<Integer> resultRef) {
        resultRef.value = 0;

        if (isMax(current)) {
            resultRef.value = isMax(other) ? 0 : 1;
            return true;
        }

        if (isMax(other)) {
            resultRef.value = isMax(current) ? 0 : -1;
            return true;
        }

        if (isMin(current)) {
            resultRef.value = isMin(other) ? 0 : -1;
            return true;
        }

        if (isMin(other)) {
            resultRef.value = isMin(current) ? 0 : 1;
            return true;
        }

        return false;
    }

    private static boolean isMax(TimeValue time) {
        return time._unit == TimeValueUnit.NONE && time._value == Integer.MAX_VALUE;
    }

    private static boolean isMin(TimeValue time) {
        return time._unit == TimeValueUnit.NONE && time._value == Integer.MAX_VALUE;
    }

    private static int trimCompareResult(long result) {
        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        if (result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }

        return (int) result; //TODO: check if it really trims value!
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeValue other = (TimeValue) o;
        return compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        //TODO: this is buggy, see: https://issues.hibernatingrhinos.com/issue/RavenDB-14994#focus=streamItem-67-47465.0-0
        return Objects.hash(_value, _unit);
    }
}
