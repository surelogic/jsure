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
  @Override
  IRNode getPromisedFor();
  AASTStatus getStatus();
  AnnotationSource getSrcType();
  AnnotationOrigin getOrigin();
  void markAsBound();
  void markAsValid();
  void markAsUnbound();
  void markAsUnassociated();
  boolean implies(IAASTRootNode other);
  boolean isSameAs(IAASTRootNode other);
  /**
   * Unparse as if it should show up in source code,
   * including the annotation and quotes
   */
  String unparseForPromise();
  
  /**
   * Get the original context that this annotation is created from
   */
  IRNode getAnnoContext();
  
  boolean isAutoGenerated();
  boolean needsConflictResolution();
}
