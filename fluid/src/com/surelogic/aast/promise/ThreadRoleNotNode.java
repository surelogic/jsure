
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

public class ThreadRoleNotNode extends AASTNode implements TRoleLit, ThreadRoleOrElem { 
  // Fields
  private final ThreadRoleNameNode target;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ThreadRoleNot") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        ThreadRoleNameNode target =  (ThreadRoleNameNode) _kids.get(0);
        return new ThreadRoleNotNode (_start,
          target        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleNotNode(int offset,
                      ThreadRoleNameNode target) {
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
    	sb.append("ThreadRoleNot\n");
    	sb.append(getTarget().unparse(debug, indent+2));
    } else {
    	sb.append("!").append(getTarget().unparse(debug, indent));
    }
    return sb.toString();
  }
  
  @Override
  public TRExpr buildTRExpr(IRNode where) {
    TRExpr res = target.buildTRExpr(null);
    return res;
  }

  /**
   * @return A non-null node
   */
  public ThreadRoleNameNode getTarget() {
    return target;
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
		return new ThreadRoleNotNode(getOffset(), (ThreadRoleNameNode)getTarget().cloneTree());
	}
}

