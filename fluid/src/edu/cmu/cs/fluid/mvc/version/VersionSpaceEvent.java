package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.version.*;

import edu.cmu.cs.fluid.mvc.*;

/**
 * This class gives extra information about an event that has happened in
 * context of a {@link VersionSpaceModel}.   This extra information contains
 * the exact tree node at which change has occured and the node which 
 * has been added to the base node. Only type of change that happens in version
 * tree is addition of a version node.
 */
public final class VersionSpaceEvent
extends ModelEvent
{
  private final Version verModified;
  private final Version verAdded;

  /**
   * Create a new event describing a newly added version.
   * @param source The model sending the event.
   * @param loc The version to which a new child was added.
   * @param added The new version added as a child to <code>loc</code>.
   */
  public VersionSpaceEvent(
    final Model source, final Version loc, final Version added)
  {
    super( source );
    verModified = loc;
    verAdded = added;
  }

  /**
   * Get the version to which a new version child was added. 
   */
  public Version getBaseVersion()
  {
    return verModified;
  }

  /**
   * Get the newly added version.
   */
  public Version getChildVersion()
  {
    return verAdded;
  }
}