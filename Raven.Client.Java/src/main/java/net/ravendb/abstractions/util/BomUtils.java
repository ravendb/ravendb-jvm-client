package net.ravendb.abstractions.util;


public class BomUtils {
  public static final String UTF8_BOM = "\uFEFF";

  public static String removeUTF8BOM(String s) {
      if (s.startsWith(UTF8_BOM)) {
          s = s.substring(1);
      }
      return s;
  }
}
