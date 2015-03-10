/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/unparse/NoToken.java,v 1.3 2007/07/05 18:15:13 aarong Exp $ */
package edu.cmu.cs.fluid.unparse;

import edu.cmu.cs.fluid.ir.IRNode;

public class NoToken extends Token {
  public final static NoToken prototype = new NoToken();

  public NoToken() { }
  @Override
  public void emit(TokenStream ts, IRNode aloc) { }
}
