package com.surelogic.aast.promise;

import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.analysis.locks.FieldKind;

public class VouchFieldIsNode extends AASTRootNode { 
  // Fields
  private final FieldKind kind;

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public VouchFieldIsNode(int offset,
		                   FieldKind kind) {
    super(offset);
    this.kind = kind;
  }
  
  /**
   * @return non-null
   */
  public FieldKind getKind() {
    return kind;
  }

  @Override
  public IAASTNode cloneTree() {
	  return new VouchFieldIsNode(offset, kind);
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
			sb.append("VouchFieldIsNode\n");
			indent(sb, indent + 2);
			sb.append("id=").append(getKind());
		} else {
			sb.append("Vouch(\"");
			sb.append(getKind());
			sb.append("\")");
		}
		return sb.toString();
  }
}

