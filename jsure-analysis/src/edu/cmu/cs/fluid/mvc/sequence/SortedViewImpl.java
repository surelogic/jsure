/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/SortedViewImpl.java,v 1.6 2003/07/15 18:39:10 thallora Exp $
 *
 * SortedSetViewImpl.java
 * Created on March 6, 2002, 4:52 PM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Minimum implementation of {@link SortedView}.
 *
 * @author Aaron Greenhouse
 */
final class SortedViewImpl
extends AbstractSortedView
implements SortedView
{
  // Checks for legitimacy of sortAttr
  public SortedViewImpl(
    final String name, final Model src, final ModelCore.Factory mf,
    final ViewCore.Factory vf, final SequenceModelCore.Factory smf,
    final AttributeInheritancePolicy policy, final String attr,
    final boolean isAsc )
  throws SlotAlreadyRegisteredException
  {
    super( name, src, mf, vf, smf, policy, attr, isAsc );
    
    /*
     * The abstract implementation has set up all our attributes.
     * Here we just have to build the model for the first time,
     * and then add the us to the list of listeners of the source model,
     * and invoke the final sanity checking method.
     */
    rebuildModel();
    srcModel.addModelListener( srcModelBreakageHandler );
    finalizeInitialization();
  }
}
