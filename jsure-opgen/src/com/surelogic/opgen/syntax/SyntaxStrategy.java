package com.surelogic.opgen.syntax;

/*
 * Callback to generate code from syntax
 */
public class SyntaxStrategy {
  protected boolean first = true;
  
  protected void init() { first = true; }
  protected void doForToken(Token token) {
	  // Nothing to do
  }
  protected void doForInfo(OpSyntax s, int i, Attribute a, String type) {
	  // Nothing to do
  }
  protected void doForChild(OpSyntax s, int i, Child c, boolean isVariable) {
	  // Nothing to do
  } 
}