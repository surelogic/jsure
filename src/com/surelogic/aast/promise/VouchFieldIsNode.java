package com.surelogic.aast.promise;

import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.analysis.locks.FieldKind;

public final class VouchFieldIsNode extends AASTRootNode { 
  // Fields
  private final FieldKind kind;
  private final String reason;

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public VouchFieldIsNode(int offset,
		                   FieldKind kind) {
    super(offset);
    this.kind = kind;
    
    // TODO: Initialized this from a parameter
    reason = "";
  }
  
  /**
   * @return non-null
   */
  public FieldKind getKind() {
    return kind;
  }
  
  public String getReason() {
    return reason;
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
      sb.append("value=").append(getKind());
      indent(sb, indent + 2);
      sb.append("reason=").append(getReason());
		} else {
		  final String reason = getReason();
			sb.append("Vouch(\"");
			if (reason.length() == 0) {
  			sb.append(getKind());
  			sb.append("\")");
			} else {
			  sb.append("Vouch(value=\"");
			  sb.append(getKind().toString());
			  sb.append("\", reason=\"");
			  sb.append(reason);
			  sb.append("\")");
			}
		}
		return sb.toString();
  }
}

