// $Header$
package com.surelogic.ast;

import com.surelogic.ast.java.operator.*;
//import com.surelogic.ast.java.promise.*;

public class JavaBinder {
  private static IJavaBinder binder;

  public static synchronized boolean hasBinder() {
    return (binder != null);
  }
  
  public static synchronized IJavaBinder getBinder() {
    if (binder == null) { throw new RuntimeException("No binder is available"); }
    return binder;
  }
  
  public static synchronized IBaseJavaBinder getBaseBinder() {
    return (IBaseJavaBinder) getBinder();
  }
  
  public static synchronized IPromiseJavaBinder getPromiseBinder() {
    return (IPromiseJavaBinder) getBinder();
  }

  public static synchronized void setBinder(IJavaBinder b) {
    if (b == null) { throw new IllegalArgumentException("Cannot call setBinder() with a null binder"); }
    binder = b;
  }
}
