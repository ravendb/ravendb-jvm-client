package net.ravendb.client.primitives;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static net.ravendb.client.primitives.DatesComparator.leftDate;
import static net.ravendb.client.primitives.DatesComparator.rightDate;
import static org.assertj.core.api.Assertions.assertThat;

public class DatesComparatorTest {

    @Test
    public void canCompareDefinedDates() {
        Date first = NetISO8601Utils.parse("2020-05-01T00:00:00.0000000");
        Date second = NetISO8601Utils.parse("2020-05-02T00:00:00.0000000");

        assertThat(DatesComparator.compare(leftDate(first), rightDate(second)))
                .isNegative();

        assertThat(DatesComparator.compare(leftDate(first), rightDate(first)))
                .isZero();
    }

    @Test
    public void canCompareDatesWithNullUsingContext() {
        Date first = NetISO8601Utils.parse("2020-05-01T00:00:00.0000000");

        assertThat(DatesComparator.compare(leftDate(first), rightDate(null)))
                .isNegative();

        assertThat(DatesComparator.compare(leftDate(null), rightDate(first)))
                .isNegative();

        assertThat(DatesComparator.compare(leftDate(null), rightDate(null)))
                .isNegative();

        assertThat(DatesComparator.compare(leftDate(null), leftDate(null)))
                .isZero();

        assertThat(DatesComparator.compare(rightDate(null), rightDate(null)))
                .isZero();
    }
}
