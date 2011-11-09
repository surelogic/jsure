package com.surelogic.opgen.syntax;

/**
 * Represents ")"
 * 
 * @author chance
 */
public class CloseParen extends SyntaxElement {
  public final int openIndex;
  CloseParen(int i, int open) {
    super(i, ")");
    openIndex = open;
  }
}
