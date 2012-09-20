package com.surelogic.dropsea.ir;

import java.util.HashMap;
import java.util.Map;

import com.surelogic.Utility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.IDrop;

@Utility
public final class DropPredicateFactory {

  private static Map<Class<?>, DropPredicate> f_type = new HashMap<Class<?>, DropPredicate>();

  /**
   * Returns a drop predicate that matches all drops that are instances of the
   * given class and its subclasses. This factory should be used rather than
   * constructing new drop predicates for this purpose because a cache is
   * maintained.
   * 
   * @param dropClass
   *          the class to match against (should be a class that is a subclass
   *          of {@link Drop}).
   * @return the drop predicate.
   */
  public static DropPredicate matchType(final Class<?> dropClass) {
    if (dropClass == null)
      throw new IllegalArgumentException(I18N.err(44, "dropClass"));

    DropPredicate result;
    synchronized (f_type) {
      result = f_type.get(dropClass);
      if (result == null) {
        result = new DropPredicate() {
          public boolean match(IDrop d) {
            /*
             * This comparison has to work for all IDropInfo instances.
             */
            return d != null ? d.instanceOfIRDropSea(dropClass) : false;
          }
        };
        f_type.put(dropClass, result);
      }
    }
    return result;
  }

  private static Map<Class<?>, DropPredicate> f_exactType = new HashMap<Class<?>, DropPredicate>();

  /**
   * Returns a drop predicate that matches all drops that are instances of the
   * given class, not including subclasses. This factory should be used rather
   * than constructing new drop predicates for this purpose because a cache is
   * maintained.
   * 
   * @param dropClass
   *          the class to match against (should be a class that is subclass of
   *          {@link Drop}).
   * @return the drop predicate.
   */
  public static DropPredicate matchExactType(final Class<?> dropClass) {
    if (dropClass == null)
      throw new IllegalArgumentException(I18N.err(44, "dropClass"));

    DropPredicate result;
    synchronized (f_exactType) {
      result = f_exactType.get(dropClass);
      if (result == null) {
        result = new DropPredicate() {
          public boolean match(IDrop d) {
            /*
             * This comparison has to work for all IDropInfo instances.
             */
            return d != null ? d.getIRDropSeaClass().equals(dropClass) : false;
          }
        };
        f_exactType.put(dropClass, result);
      }
    }
    return result;
  }
}
