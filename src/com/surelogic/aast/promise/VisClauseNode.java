/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/VisClauseNode.java,v 1.1 2007/10/27 17:11:10 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.analysis.colors.CBinExpr;
import com.surelogic.analysis.colors.CExpr;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

public class VisClauseNode extends ModuleAnnotationNode {
  // Fields
  private final String modName;

  public static final AbstractSingleNodeFactory factory = new AbstractSingleNodeFactory(
      "VisClause") {
    @Override
    @SuppressWarnings("unchecked")
    public AASTNode create(String _token, int _start, int _stop, int _mods,
                           String _id, int _dims, List<AASTNode> _kids) {
      return new VisClauseNode(_start, _id);
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be
   * 
   * @unique
   */
  public VisClauseNode(int offset, String optionalName) {
    super(offset);

    if (optionalName == null) {
      modName = "".intern();
    } else {
      modName = optionalName;
    }
  }

  
  
  
  public String getModName() {
    return modName;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("VisClause ");
    sb.append(modName);
    sb.append('\n');
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
    return new VisClauseNode(getOffset(), modName);
  }
  


}
