/*$Header$*/
package com.surelogic.aast.promise;

public class AssumeScopedPromiseNode extends ScopedPromiseNode {
  public AssumeScopedPromiseNode(int offset, String promise, PromiseTargetNode targets) {
    super(offset, promise, targets);
  }
  
  @Override
  public final String unparse(boolean debug, int indent) {
	  return unparse(debug, indent, "Assume");
  }
}
