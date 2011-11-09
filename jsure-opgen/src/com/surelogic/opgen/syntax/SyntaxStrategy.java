package com.surelogic.opgen.syntax;

/*
 * Callback to generate code from syntax
 */
public class SyntaxStrategy {
  protected boolean first = true;
  
  protected void init() { first = true; }
  protected void doForToken(Token token) {}
  protected void doForInfo(OpSyntax s, int i, Attribute a, String type) {}
  protected void doForChild(OpSyntax s, int i, Child c, boolean isVariable) {}
}