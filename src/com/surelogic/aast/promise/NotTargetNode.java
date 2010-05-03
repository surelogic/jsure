
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

public class NotTargetNode extends ComplexTargetNode { 
  // Fields
  private final PromiseTargetNode target;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("NotTarget") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        PromiseTargetNode target =  (PromiseTargetNode) _kids.get(0);
        return new NotTargetNode (_start,
          target        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public NotTargetNode(int offset,
                       PromiseTargetNode target) {
    super(offset);
    if (target == null) { throw new IllegalArgumentException("target is null"); }
    ((AASTNode) target).setParent(this);
    this.target = target;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
      indent(sb, indent); 
      sb.append("NotTarget\n");
      sb.append(getTarget().unparse(debug, indent+2));
    } else {
      sb.append('!');
      sb.append(getTarget());
    }
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public PromiseTargetNode getTarget() {
    return target;
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
		return !target.matches(irNode);
	}
	
	@Override
	public Operator appliesTo() {
		return target.appliesTo();
	}
	
  @Override
  public IAASTNode cloneTree(){
  	return new NotTargetNode(getOffset(), (PromiseTargetNode)getTarget().cloneTree());
  }
}

