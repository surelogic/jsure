package com.surelogic.opgen.syntax;

/**
 * Represents "("
 * 
 * @author chance
 */
public class OpenParen extends SyntaxElement {
  public final int closeIndex;
  OpenParen(int i, int close) {
    super(i, "(");
    closeIndex = close;
  }  
}
