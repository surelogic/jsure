package edu.cmu.cs.fluid.mvc.version;


import edu.cmu.cs.fluid.mvc.tree.*;

/**
 * Interface to a stateful view that views a {@link VersionSpaceModel} and
 * exports a {@link VersionTrackerModel}.  The attribute {@link #IS_FOLLOWING}
 * determines whether the version in the VersionTrackerModel follows when a 
 * new child of the currently referenced version is added to the
 * VersionSpaceModel.  Such functionality is desirable for keeping the 
 * cursor pointing to the current point of modification, for example.
 * The {@link VersionTrackerModel#VERSION} attribute is constrained to only
 * accept versions that are part of the source version space.
 *
 * <P>An implementation must support the model-level attributes:
 * <ul>
 * <li>{@link #MODEL_NAME}
 * <li>{@link #MODEL_NODE}
 * <li>{@link VersionTrackerModel#VERSION}
 * <li>{@link #IS_FOLLOWING}
 * </ul>
 *
 * <P>An implementation must support the node-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * </ul>
 *
 * @author Aaron Greenhouse
 * @author Zia Syed
 */
public interface VersionSpaceToVersionTrackerStatefulView
extends ForestToModelStatefulView, VersionTrackerModel
{
  /**
   * Mutable {@link Boolean}-valued model-level attribute that when
   * <code>true</code> indicates new "buds" on the version space tree should
   * be followed by the exported version tracker.
   */
  public static final String IS_FOLLOWING =
    "VersionSpaceToVersionTrackerStatefulView.IS_FOLLOWING";
	
  /**
   * Convienence method for setting the {@link #IS_FOLLOWING} attribute.
   */
  public void setFollowing( boolean mode );
	
  /**
   * Convienence method for getting the value of the
   * {@link #IS_FOLLOWING attribute}
   */
  public boolean isFollowing();
}
