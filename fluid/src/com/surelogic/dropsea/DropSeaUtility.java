package com.surelogic.dropsea;

import java.util.Map;

import com.surelogic.Utility;

@Utility
public final class DropSeaUtility {

  public static boolean isSameProposalAs(IProposedPromiseDrop o1, IProposedPromiseDrop o2) {
    if (o1 == null && o2 == null)
      return true;
    if (o1 == null && o2 != null)
      return false;
    if (o1 != null && o2 == null)
      return false;

    return isSame(o1.getAnnotation(), o2.getAnnotation()) && isSame(o1.getValue(), o2.getValue())
        && isSame(o1.getReplacedValue(), o2.getReplacedValue()) && isSame(o1.getJavaRef(), o2.getJavaRef())
        && isAllSame(o1.getAttributes(), o2.getAttributes())
        && isAllSame(o1.getReplacedAttributes(), o2.getReplacedAttributes());
  }

  private static boolean isAllSame(Map<String, String> m1, Map<String, String> m2) {
    return isSame(m1, m2);
  }

  private static <T> boolean isSame(T o1, T o2) {
    if (o1 == null) {
      if (o2 != null) {
        return false;
      }
    } else if (!o1.equals(o2)) {
      return false;
    }
    return true;
  }

  private DropSeaUtility() {
    // no instances
  }
}
