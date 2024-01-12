package net.ravendb.client.primitives;

import java.util.Date;

public class DatesComparator {

    public static int compare(DateWithContext lhs, DateWithContext rhs) {
        if (lhs.getDate() != null && rhs.getDate() != null) {
            return lhs.getDate().compareTo(rhs.getDate());
        }

        // lhr or rhs is null - unify values using context
        long leftValue = lhs.getDate() != null
                ? lhs.getDate().getTime()
                : (lhs.getContext() == DateContext.FROM ? Long.MIN_VALUE : Long.MAX_VALUE);

        long rightValue = rhs.getDate() != null
                ? rhs.getDate().getTime()
                : (rhs.getContext() == DateContext.FROM ? Long.MIN_VALUE : Long.MAX_VALUE);

        return Long.compare(leftValue, rightValue);
    }

    public static class DateWithContext {
        private final Date _date;
        private final DateContext _context;

        public DateWithContext(Date date, DateContext context) {
            _date = date;
            _context = context;
        }

        public Date getDate() {
            return _date;
        }

        public DateContext getContext() {
            return _context;
        }
    }

    public enum DateContext {
        FROM,
        TO
    }

    /*
      Date or MinDate if null
     */
    public static DateWithContext leftDate(Date date) {
        return new DateWithContext(date, DateContext.FROM);
    }

    /*
     Date or MaxDate if null
     */
    public static DateWithContext rightDate(Date date) {
        return new DateWithContext(date, DateContext.TO);
    }

    public static DateWithContext definedDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        return new DateWithContext(date, DateContext.TO);
    }
}
