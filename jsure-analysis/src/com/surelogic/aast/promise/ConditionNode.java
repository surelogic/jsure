
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class ConditionNode extends AASTNode { 
  // Fields
  private final ExpressionNode cond;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("Condition") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ExpressionNode cond =  (ExpressionNode) _kids.get(0);
        return new ConditionNode (_start,
          cond        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ConditionNode(int offset,
                       ExpressionNode cond) {
    super(offset);
    if (cond == null) { throw new IllegalArgumentException("cond is null"); }
    ((AASTNode) cond).setParent(this);
    this.cond = cond;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("Condition\n");
    sb.append(getCond().unparse(debug, indent+2));
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ExpressionNode getCond() {
    return cond;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new ConditionNode(getOffset(), (ExpressionNode)getCond().cloneOrModifyTree(mod));
  }
}

