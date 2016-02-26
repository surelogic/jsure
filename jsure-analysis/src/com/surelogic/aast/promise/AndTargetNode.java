
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

public class AndTargetNode extends ComplexTargetNode { 
  // Fields
  private final PromiseTargetNode target1;
  private final PromiseTargetNode target2;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("AndTarget") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        PromiseTargetNode target1 =  (PromiseTargetNode) _kids.get(0);
        PromiseTargetNode target2 =  (PromiseTargetNode) _kids.get(1);
        return new AndTargetNode (_start,
          target1,
          target2        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public AndTargetNode(int offset,
                       PromiseTargetNode target1,
                       PromiseTargetNode target2) {
    super(offset);
    if (target1 == null) { throw new IllegalArgumentException("target1 is null"); }
    ((AASTNode) target1).setParent(this);
    this.target1 = target1;
    if (target2 == null) { throw new IllegalArgumentException("target2 is null"); }
    ((AASTNode) target2).setParent(this);
    this.target2 = target2;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
      indent(sb, indent); 
      sb.append("AndTarget\n");
      sb.append(getTarget1().unparse(debug, indent+2));
      sb.append(getTarget2().unparse(debug, indent+2));
    } else {
      sb.append(getTarget1());
      sb.append(" & ");
      sb.append(getTarget2());
    }
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public PromiseTargetNode getTarget1() {
    return target1;
  }
  /**
   * @return A non-null node
   */
  public PromiseTargetNode getTarget2() {
    return target2;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }

	/* (non-Javadoc)
	 * @see com.surelogic.aast.promise.PromiseTargetNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode irNode) {
		return target1.matches(irNode) && target2.matches(irNode);
	}
	
	@Override
	public Operator appliesTo() {
		return combineOperators(target1.appliesTo(), target2.appliesTo());
	}
	
	@Override
	public AndTargetNode internalClone(final INodeModifier mod) {
		return new AndTargetNode(getOffset(), (PromiseTargetNode)getTarget1().cloneOrModifyTree(mod), (PromiseTargetNode)getTarget2().cloneOrModifyTree(mod));
	}
}

