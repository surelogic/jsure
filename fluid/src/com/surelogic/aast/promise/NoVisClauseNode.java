/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/NoVisClauseNode.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class NoVisClauseNode extends ModuleAnnotationNode {

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "NoVisClause") {
    @Override
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      return new NoVisClauseNode(_start);
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be
   * 
   * @unique
   */
  public NoVisClauseNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("NoVisClause\n");
    return sb.toString();
  }
 
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }

  /* (non-Javadoc)
   * @see com.surelogic.aast.AASTNode#cloneTree()
   */
  @Override
  public IAASTNode cloneTree() {
    return new NoVisClauseNode(getOffset());
  }
  



}
