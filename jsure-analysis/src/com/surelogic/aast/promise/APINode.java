
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

public class APINode extends AASTNode { 
  // Fields
  private final String id;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("API") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String id = _id;
        return new APINode (_start,
          id        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public APINode(int offset,
                 String id) {
    super(offset);
    if (id == null) { throw new IllegalArgumentException("id is null"); }
    this.id = id;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("API\n");
    indent(sb, indent+2);
    sb.append("id=").append(getId());
    sb.append("\n");
    return sb.toString();
  }

  /**
   * @return A non-null String
   */
  public String getId() {
    return id;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  protected APINode internalClone(final INodeModifier mod) {
  	return new APINode(getOffset(), getId());
  }
}

