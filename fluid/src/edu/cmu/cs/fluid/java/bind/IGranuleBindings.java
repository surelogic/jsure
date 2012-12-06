/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IGranuleBindings.java,v 1.3 2007/09/18 21:24:42 chance Exp $*/
package edu.cmu.cs.fluid.java.bind;

import java.util.List;

import com.surelogic.ThreadSafe;

import edu.cmu.cs.fluid.derived.IDerivedInformation;
import edu.cmu.cs.fluid.ir.*;

/**
 * A hunk of completed binding information.
 * The granularity is determined by what can be done to avoid circular derivation.
 * @author boyland
 */
@ThreadSafe
public interface IGranuleBindings extends IDerivedInformation {
  /**
   * Return the slot info for method override information.
   * @return the method override information.
   */
  SlotInfo<List<IBinding>> getMethodOverridesAttr();
  
  /**
   * Return the slot that stores the name to declaration binding information.
   * @return the binding override information
   */
  SlotInfo<IBinding> getUseToDeclAttr();
  
  /**
   * A badly designed interface that will be removed as soon as I can do it safely.
   * @param node
   * @deprecated use @{link #ensureDerived()}
   */
  @Deprecated
  void ensureDerived(IRNode node);
  
  boolean containsFullInfo();
  
  boolean isDestroyed();
  
  void destroy();
  
  IRNode getNode();
}
