package com.surelogic.aast.promise;

import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;
import com.surelogic.analysis.concurrency.heldlocks.FieldKind;

public final class VouchFieldIsNode extends AASTRootNode { 
  public static final String NO_REASON = "";
  
  public static final String REASON = "reason";
	
  // Fields
  private final FieldKind kind;
  private final String reason;

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public VouchFieldIsNode(int offset,
		                   FieldKind kind,
		                   String reason) {
    super(offset);
    this.kind = kind;
    this.reason = reason == null ? NO_REASON : reason;
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
	  return new VouchFieldIsNode(offset, kind, reason);
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
  			sb.append(getKindAsString());
  			sb.append("\")");
			} else {
			  sb.append("Vouch(value=\"");
			  sb.append(getKindAsString());
			  sb.append("\", reason=\"");
			  sb.append(reason);
			  sb.append("\")");
			}
		}
		return sb.toString();
  }
  
  private String getKindAsString() {
	  FieldKind k = getKind();
	  if (k == FieldKind.Final) {
		  return "final";
	  }
	  return k.toString();
  }
  
  public String unparseForPromise() {
	  return unparse(false);  
  }
}

