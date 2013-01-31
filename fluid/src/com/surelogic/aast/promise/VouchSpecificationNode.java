package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;

public class VouchSpecificationNode extends TargetedAnnotationNode  
{ 
  // Fields
  private final String reason;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("VouchSpecification") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String reason = _id;
        return new VouchSpecificationNode (_start, reason);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public VouchSpecificationNode(int offset, String reason) {
    super(offset);
    if (reason == null) { throw new IllegalArgumentException("promise is null"); }
    String p = reason;
    if (reason.startsWith("'") || reason.startsWith("\"")) {
      p = reason.substring(1, reason.length()-1);
    }
    this.reason = p;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
      indent(sb, indent); 
      sb.append("Vouch\n");
      indent(sb, indent+2);
      sb.append("reason=").append(getReason());
    } else {
      sb.append("Vouch(");
      sb.append('\"').append(getReason()).append('\"');
      sb.append(')');
    }
    return sb.toString();
  }

  /**
   * @return A non-null String
   */
  public String getReason() {
    return reason;
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }

  @Override
  public IAASTNode cloneTree() {
	  return new VouchSpecificationNode(offset, reason);
  }

  @Override
  public boolean implies(IAASTRootNode other) {
	  return false; // TODO Cannot be matched
  }
}

