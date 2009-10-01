
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class ScopedPromiseNode extends TargetedAnnotationNode 
implements IAASTRootNode 
{ 
  // Fields
  private final String promise;
  private final PromiseTargetNode targets;

  public static final AbstractSingleNodeFactory factory =
    new AbstractSingleNodeFactory("ScopedPromise") {
      @Override
      @SuppressWarnings("unchecked")      public AASTNode create(String _token, int _start, int _stop,
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

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
      indent(sb, indent); 
      sb.append("ScopedPromise\n");
      indent(sb, indent+2);
      sb.append("promise=").append(getPromise());
      sb.append("\n");
      sb.append(getTargets().unparse(debug, indent+2));
    } else {
      sb.append('@').append(getPromise());
      if (!(getTargets() instanceof AnyTargetNode)) {
          sb.append(" for ").append(getTargets());
      }
    }
    return sb.toString();
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
  public IAASTNode cloneTree(){
  	return new ScopedPromiseNode(getOffset(), new String(getPromise()), (PromiseTargetNode)getTargets().cloneTree());
  }
}

