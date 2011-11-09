/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/unparse/Combined.java,v 1.5 2007/07/05 18:15:13 aarong Exp $ */
package edu.cmu.cs.fluid.unparse;

import edu.cmu.cs.fluid.ir.IRNode;

public class Combined extends Token {
  private final Token token1, token2;
  public Combined(Token tok1, Token tok2) {
    token1 = tok1;
    token2 = tok2;
  }
  @Override
  public void emit(TokenStream ts, IRNode aloc) {
    token1.emit(ts,aloc);
    token2.emit(ts,aloc);
  }
}
