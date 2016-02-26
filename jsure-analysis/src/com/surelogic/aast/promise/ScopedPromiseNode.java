
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

public class ScopedPromiseNode extends TargetedAnnotationNode 
{ 
  // Fields
  private final String promise;
  private final PromiseTargetNode targets;
  private ScopedPromiseNode subsumedBy;
  
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("ScopedPromise") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        String promise = _id;
        PromiseTargetNode targets =  (PromiseTargetNode) _kids.get(0);
        return new ScopedPromiseNode (_start,
          promise,
          targets        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ScopedPromiseNode(int offset,
                           String promise,
                           PromiseTargetNode targets) {
    super(offset);
    if (promise == null) { throw new IllegalArgumentException("promise is null"); }
    String p = promise;
    if (promise.startsWith("'") || promise.startsWith("\"")) {
      p = promise.substring(1, promise.length()-1);
    }
    this.promise = p;
    if (targets == null) { throw new IllegalArgumentException("targets is null"); }
    ((AASTNode) targets).setParent(this);
    this.targets = targets;
  }

  protected String unparse(boolean debug, int indent, String name) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
      indent(sb, indent); 
      sb.append(name).append("\n");
      indent(sb, indent+2);
      sb.append("promise=").append(getPromise());
      sb.append("\n");
      sb.append(getTargets().unparse(debug, indent+2));
    } else {
      sb.append(name).append("(\"");
      sb.append('@').append(getPromise());
      if (!(getTargets() instanceof AnyTargetNode)) {
          sb.append(" for ").append(getTargets());
      }
      sb.append("\")");
    }
    return sb.toString();
  }

  @Override
  public String unparse(boolean debug, int indent) {
	  return unparse(debug, indent, "Promise");
  }
  
  /**
   * @return A non-null String
   */
  public String getPromise() {
    return promise;
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
  	return new ScopedPromiseNode(getOffset(), getPromise(), (PromiseTargetNode)getTargets().cloneOrModifyTree(mod));
  }

  public void markAsSubsumed(ScopedPromiseNode wildcard) {
	  subsumedBy = wildcard;
  }
  
  public ScopedPromiseNode subsumedBy() {
	  return subsumedBy;
  }
}

