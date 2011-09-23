/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/IAASTRootNode.java,v 1.7 2007/09/24 21:09:56 ethan Exp $*/
package com.surelogic.aast;

import com.surelogic.annotation.AnnotationSource;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.IHasPromisedFor;

/**
 * A root node for an AAST
 * (e.g. getParent() returns null)
 *
 * @author Edwin.Chan
 */
public interface IAASTRootNode extends IAASTNode, IHasPromisedFor {
  void clearPromisedFor();
  IRNode getPromisedFor();
  AASTStatus getStatus();
  AnnotationSource getSrcType();
  void markAsBound();
  void markAsValid();
  void markAsUnbound();
  void markAsUnassociated();
  boolean implies(IAASTRootNode other);
  boolean isSameAs(IAASTRootNode other);
}
