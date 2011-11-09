/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/JavaUnparser.java,v 1.8 2005/07/07 10:03:48 boyland Exp $ */
package edu.cmu.cs.fluid.java;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.unparse.TokenStream;

public interface JavaUnparser extends TokenStream {
  public abstract void unparse(IRNode node);
  public abstract JavaUnparseStyle getStyle();
  public abstract SyntaxTreeInterface getTree();
  public abstract boolean isImplicit(IRNode node);
}
