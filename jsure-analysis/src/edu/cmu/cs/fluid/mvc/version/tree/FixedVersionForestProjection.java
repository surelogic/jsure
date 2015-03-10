package edu.cmu.cs.fluid.mvc.version.tree;

import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.mvc.tree.ForestToForestStatefulView;
import edu.cmu.cs.fluid.mvc.version.ModelAndVersionTrackerToModelStatefulView;
import edu.cmu.cs.fluid.mvc.version.VersionTrackerModel;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Specialization of {@link ModelAndVersionTrackerToModelStatefulView} that
 * also includes
 * the {@link ForestToForestStatefulView} interface.  That is, it is 
 * specifically a projection of a versioned ForestModel.

 *
 * <P>An implementation of this interface must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#VIEW_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.View#SRC_MODELS}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#ROOTS}
 * </ul>
 *
 * <p>The values of the MODEL_NAME and VIEW_NAME attributes do not
 * need to be the same.
 *
 * <P>An implementation must support the node-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.DigraphModel#CHILDREN}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.SymmetricDigraphModel#PARENTS}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#LOCATION}
 * </ul>
 *
 * @author Aaron Greenhouse
 */
public interface FixedVersionForestProjection
extends ModelAndVersionTrackerToModelStatefulView, ForestToForestStatefulView
{
  /**
   * Interface for factories that create instances of 
   * {@link FixedVersionForestProjection}.
   */
  public static interface Factory
  {
    /**
     * Create a new instance of {@link FixedVersionForestProjection}.
     * @param name The name to give to the new model.
     * @param srcModel the versioned forest model to create a projection of.
     * @param tracker The version-cursor model that will control the version
     *               at which <code>srcModel</code> is projected.
     */
    public FixedVersionForestProjection create(
      String name, ForestModel srcModel, VersionTrackerModel tracker )
    throws SlotAlreadyRegisteredException;
  }
}
