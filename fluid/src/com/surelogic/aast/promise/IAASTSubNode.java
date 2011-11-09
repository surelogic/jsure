/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/IAASTSubNode.java,v 1.1 2007/10/19 20:32:18 dfsuther Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.INodeVisitor;

public interface IAASTSubNode {
  public String unparse(boolean debug, int indent);

  public <T> T accept(INodeVisitor<T> visitor);

  public IAASTNode cloneTree();

}
