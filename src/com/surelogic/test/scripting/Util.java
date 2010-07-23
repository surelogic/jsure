package com.surelogic.test.scripting;

import java.util.*;

public class Util {
  private static final String[] noTokens = new String[0];
	
  /**
   * Splits the line into tokens based on delim
   * 
   * @return non-null
   */
  public static String[] collectTokens(String line, String delim) {
    final StringTokenizer st = new StringTokenizer(line, delim);
    List<String> l = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      l.add(st.nextToken());
    }
    if (l.isEmpty()) {
      return noTokens;
    }
    return l.toArray(new String[l.size()]);
  }
}
