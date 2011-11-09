/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/PackageScopedPromiseNode.java,v 1.1 2007/10/08 21:36:00 chance Exp $*/
package com.surelogic.aast.promise;

public class PackageScopedPromiseNode extends ScopedPromiseNode {
  public PackageScopedPromiseNode(int offset, String promise,
                                  PromiseTargetNode targets) {
    super(offset, promise, targets);
  }
}
