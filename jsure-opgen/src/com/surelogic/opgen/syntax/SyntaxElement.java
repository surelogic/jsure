package com.surelogic.opgen.syntax;

public abstract class SyntaxElement {
  public final int index;
  public final String text;
  
  SyntaxElement(int i, String t) {
    index = i;
    text = t;
  }
  
  @Override
  public String toString() {
    return text;
  }
  
  /*
  @Override
  public boolean equals(Object o) {
    if (o instanceof SyntaxElement) {
      SyntaxElement e2 = (SyntaxElement) o;
      if (text.equals(e2.text)) {
        return true;
      }
    }
    return false;
  }
  @Override
  public int hashCode() {
    return text.hashCode();
  }
  */
}
