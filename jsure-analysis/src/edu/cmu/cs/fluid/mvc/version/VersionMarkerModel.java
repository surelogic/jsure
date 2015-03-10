package edu.cmu.cs.fluid.mvc.version;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.version.*;

/**
 * A pure specialization of {@link VersionTrackerModel}.  
 * Simpy encapsulates the version.  This model is roughly analogous to 
 * {@link edu.cmu.cs.fluid.version.VersionMarker}.
 *
 * <P>An implementation must support the model-level attributes:
 * <ul>
 * <li>{@link #MODEL_NAME}
 * <li>{@link #MODEL_NODE}
 * <li>{@link #VERSION}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * </ul>
 *
 * @author Aaron Greenhouse
 * @author Zia Syed
 */
public interface VersionMarkerModel
extends VersionTrackerModel
{
  /**
   * Factory interface for creating instances of {@link VersionMarkerModel}.
   */
  public static interface Factory
  {
    /**
     * Create a new VersionMarker model that initially stores the 
     * given version.
     * @param name The name of the model.
     * @param version The initial version to store in the model.
     */
    public VersionMarkerModel create( String name, Version version )
    throws SlotAlreadyRegisteredException;
  }
}
