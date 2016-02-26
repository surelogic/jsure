
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

// i.e. 'nothing'
public class NoTargetNode extends ComplexTargetNode { 

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("NoTarget") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        return new NoTargetNode (_start);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public NoTargetNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {

    if (debug) { 
      StringBuilder sb = new StringBuilder();
      indent(sb, indent); 
      sb.append("NoTarget\n");
      return sb.toString();
    } else {
      return "nothing";
    }
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
		return false;
	}
	
	@Override
	public Operator appliesTo() {
		return null;
	}
	
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new NoTargetNode(getOffset());
  }
}

