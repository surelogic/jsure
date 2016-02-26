/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/IAASTNode.java,v 1.3 2007/09/24 21:09:56 ethan Exp $*/
package com.surelogic.aast;

import edu.cmu.cs.fluid.ir.IRNode;

public interface IAASTNode {
  public IAASTNode getParent();
  public IAASTRootNode getRoot();

  /**
   * @return The offset into the source text
   */
  public int getOffset();
  
  /**
   * Create a textual representation of the AST
   * @param debug true if for debugging, false if for pretty-printing
   * @return A String with newlines
   */
  public String unparse(boolean debug);
  
  public String unparse(boolean debug, int indent);
  
  public <T> T accept(INodeVisitor<T> visitor);
  
  public IRNode getPromisedFor();
  public IRNode getAnnoContext();
  
  public IAASTNode cloneTree();
  
  /**
   * This is meant to clone the given AAST, if there are  
   * nodes modified by 'mod', and otherwise return the 
   * original AAST
   */
  public IAASTNode modifyTree(INodeModifier mod);  
}
