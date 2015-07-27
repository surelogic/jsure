package com.surelogic.xml;

public class MergeResult<T extends IJavaElement> {
  public final T element;
  public final boolean isModified;

  MergeResult(T e, boolean mod) {
    element = e;
    isModified = mod;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static final MergeResult nullResult = new MergeResult(null, false);

  static <T extends IJavaElement> MergeResult<T> nullResult() {
    return nullResult;
  }
}
