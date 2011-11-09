
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;

public class InvariantDeclarationNode extends AASTNode { 
  // Fields
  private final int mods;
  private final String id;
  private final ConditionNode cond;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("InvariantDeclaration") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        int mods = _mods;
        String id = _id;
        ConditionNode cond =  (ConditionNode) _kids.get(0);
        return new InvariantDeclarationNode (_start,
          mods,
          id,
          cond        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public InvariantDeclarationNode(int offset,
                                  int mods,
                                  String id,
                                  ConditionNode cond) {
    super(offset);
    this.mods = mods;
    if (id == null) { throw new IllegalArgumentException("id is null"); }
    this.id = id;
    if (cond == null) { throw new IllegalArgumentException("cond is null"); }
    ((AASTNode) cond).setParent(this);
    this.cond = cond;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("InvariantDeclaration\n");
    indent(sb, indent+2);
    sb.append("mods=").append(getMods());
    sb.append("\n");
    indent(sb, indent+2);
    sb.append("id=").append(getId());
    sb.append("\n");
    sb.append(getCond().unparse(debug, indent+2));
    return sb.toString();
  }
  
  /**
   * @return A non-null int
   */
  public int getMods() {
    return mods;
  }
  /**
   * @return A non-null String
   */
  public String getId() {
    return id;
  }
  /**
   * @return A non-null node
   */
  public ConditionNode getCond() {
    return cond;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new InvariantDeclarationNode(getOffset(), getMods(), new String(getId()), (ConditionNode)cond.cloneTree());
  }
}

