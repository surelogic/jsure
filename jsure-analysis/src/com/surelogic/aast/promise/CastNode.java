package com.surelogic.aast.promise;

import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.parse.JJNode;

public final class CastNode extends AASTRootNode { 
  public enum CastKind {
	  toNonNull, toNullable
  }
	
  // Fields
  private final CastKind kind;

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public CastNode(int offset, CastKind kind) {
    super(offset);
    this.kind = kind;
  }
  
  /**
   * @return non-null
   */
  public CastKind getKind() {
    return kind;
  }

  @Override
  public IAASTNode cloneTree() {
	  return new CastNode(offset, kind);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
	  return visitor.visit(this);
  }

  @Override
  public String unparse(boolean debug, int indent) {
	  StringBuilder sb = new StringBuilder();
	  if (debug) {
		  indent(sb, indent);
		  sb.append("CastNode\n");
		  indent(sb, indent + 2);
		  sb.append("value=").append(getKind());
	  } else {
		  MethodCall op = (MethodCall) JJNode.tree.getOperator(getPromisedFor());
		  IRNode args = op.get_Args(getPromisedFor());
		  sb.append("Cast.").append(getKind()).append(DebugUnparser.toString(args));
	  }
	  return sb.toString();
  }
  
  @Override
  public String unparseForPromise() {
	  return unparse(false);  
  }
}

