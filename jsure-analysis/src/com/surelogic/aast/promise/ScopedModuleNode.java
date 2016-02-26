
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

public class ScopedModuleNode extends ModuleNode { 
  // Fields
  private final PromiseTargetNode targets;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ScopedModule") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String id = _id;
        PromiseTargetNode targets =  (PromiseTargetNode) _kids.get(0);
        return new ScopedModuleNode (_start,
          id,
          targets        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ScopedModuleNode(int offset,
                          String id,
                          PromiseTargetNode targets) {
    super(offset, id);
    if (targets == null) { throw new IllegalArgumentException("targets is null"); }
    ((AASTNode) targets).setParent(this);
    this.targets = targets;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("ScopedModule\n");
    indent(sb, indent+2);
    sb.append("id=").append(getId());
    sb.append("\n");
    sb.append(getTargets().unparse(debug, indent+2));
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public PromiseTargetNode getTargets() {
    return targets;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new ScopedModuleNode(getOffset(), getId(), (PromiseTargetNode)getTargets().cloneOrModifyTree(mod));
  }
}

