package net.ravendb.client.primitives;

import java.text.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * Provides support for 7 characters in date fraction. Ex. :  2008-04-10T06:30:00.0000000-07:00
 */
public class NetDateFormat extends DateFormat {
  private static final long serialVersionUID = 1L;

  // those classes are to try to allow a consistent behavior for hashcode/equals and other methods
  private static final Calendar CALENDAR = new GregorianCalendar();
  private static final NumberFormat NUMBER_FORMAT = new DecimalFormat();

  public NetDateFormat() {
      this.numberFormat = NUMBER_FORMAT;
      this.calendar = CALENDAR;
  }

  @Override
  public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition)
  {
      String value = NetISO8601Utils.format(date, true);
      toAppendTo.append(value);
      return toAppendTo;
  }

  @Override
  public Date parse(String source, ParsePosition pos)
  {
      // index must be set to other than 0, I would swear this requirement is not there in
      // some version of jdk 6.
      pos.setIndex(source.length());
      return NetISO8601Utils.parse(source);
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public Object clone() {
      return this;    // jackson calls clone every time. We are threadsafe so just returns the instance
  }
}
