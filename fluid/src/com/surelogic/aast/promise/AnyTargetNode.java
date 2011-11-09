package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

public class AnyTargetNode extends ComplexTargetNode { 
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("AnyTarget") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        return new AnyTargetNode (_start);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public AnyTargetNode(int offset) {
    super(offset);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
      indent(sb, indent); 
      sb.append("AnyTarget\n");
    } else {
      return "ANY";
    }
    return sb.toString();
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }

	@Override
	public Operator appliesTo() {
		return Operator.prototype;
	}
		
	@Override
	public boolean matches(IRNode irNode) {
		return irNode != null;
	}
	
	@Override
	public IAASTNode cloneTree(){
		return new AnyTargetNode(getOffset());
	}
}

