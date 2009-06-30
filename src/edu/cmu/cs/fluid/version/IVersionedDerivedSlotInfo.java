/*
 * Created on Jul 8, 2003
 *
 */
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRRegion;

/**
 * @author mbendary
 *
 * An interface for VersionedDerivedSlotInfo and its unversioned counterpart.
 * All setting of slots must go on between {@link #productionStarted()} and
 * {@link #productionDone()} calls, and one can only get the values of slots
 * that were set in this way.  For any other version, the slot is undefined.
 * <p>
 * This class doesn't work properly.
 * @deprecated use BiVersioned slots internally inside a VersionedInformation.
 */
@Deprecated
public interface IVersionedDerivedSlotInfo {

  /** Note that we will start updating the slot.  Until production done,
   * This slot will behave as a simple slot for nodes in the given region.
   * @param reg region of nodes for which we are producing values.
   * If a versioned region, we perform the operation for
   * all regions of the versioned region.
   */
  public void productionStarted(IRRegion reg);

  /** Record that the values for the current version and
   * this region are now valid.
   */
  public void productionDone(IRRegion reg);
  
  /** Return whether information has been computed for the current
   * version for this node, assuming the versioned structure is OK.
   * We check to see if the version is new in which case it
   * must be produced or in production.
   */
  public boolean isProduced(IRNode node);
  
}
