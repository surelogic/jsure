package edu.cmu.cs.fluid.mvc.version.tree.syntax;

import edu.cmu.cs.fluid.mvc.tree.syntax.SyntaxForestModel;
import edu.cmu.cs.fluid.mvc.tree.syntax.SyntaxForestToSyntaxForestStatefulView;
import edu.cmu.cs.fluid.mvc.version.VersionTrackerModel;
import edu.cmu.cs.fluid.mvc.version.tree.FixedVersionForestProjection;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;

/**
 * Specialization of {@link FixedVersionForestProjection} that
 * also includes the {@link SyntaxForestToSyntaxForestStatefulView} interface.
 *  That is, it is specifically a projection of a versioned SyntaxForestModel.
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
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.DigraphModel#CHILDREN}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.SymmetricDigraphModel#PARENTS}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#LOCATION}
 * <li>{@link SyntaxForestModel#OPERATOR}
 * </ul>
 */
public interface FixedVersionSyntaxForestProjection
extends FixedVersionForestProjection, SyntaxForestToSyntaxForestStatefulView
{
  /**
   * Interface for factories that create instances of 
   * {@link FixedVersionSyntaxForestProjection}.
   */
  public static interface Factory
  {
    /**
     * Create a new instance of {@link FixedVersionSyntaxForestProjection}.
     * @param name The name to give to the new model.
     * @param srcModel the versioned syntax forest model to create a projection of.
     * @param tracker The version-cursor model that will control the version
     *               at which <code>srcModel</code> is projected.
     */
    public FixedVersionSyntaxForestProjection create(
      String name, SyntaxForestModel srcModel, VersionTrackerModel tracker )
    throws SlotAlreadyRegisteredException;
  }
}
