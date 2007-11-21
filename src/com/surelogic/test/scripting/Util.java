package com.surelogic.test.scripting;

import java.util.*;

public class Util {
  public static String[] collectTokens(String line, String delim) {
    final StringTokenizer st = new StringTokenizer(line, delim);
    List<String> l = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      l.add(st.nextToken());
    }
    if (l.isEmpty()) {
      return null;
    }
    return l.toArray(new String[l.size()]);
  }
}
