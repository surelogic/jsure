// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/ConfigurableSyntaxForestView.java,v 1.15 2006/03/29 20:08:42 chance Exp $
package edu.cmu.cs.fluid.mvc.tree.syntax;

import java.util.Set;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.tree.ConfigurableForestView;
import edu.cmu.cs.fluid.mvc.tree.ForestEllipsisPolicy;
import edu.cmu.cs.fluid.mvc.tree.ForestProxyAttributePolicy;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;


/**
 * A view of an <em>unversioned</em> syntax forest that allows for 
 * nodes to be ellided, and the display mode to be
 * altered.  The exported model can contain nodes
 * for which {@link #isEllipsis} is <code>true</code>.
 *
 * <p>See {@link ConfigurableForestView} for a description of how the 
 * exported model can be configured.
 *
 * <P>An implementation must support the 
 * model-level attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NAME}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#MODEL_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#ROOTS}
 * <li>{@link ConfigurableForestView#ELLIPSIS_POLICY}
 * <li>{@link ConfigurableForestView#VIEW_MODE}
 * </ul>
 *
 * <P>An implementation must support the node-level
 * attributes:
 * <ul>
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#IS_ELLIPSIS}
 * <li>{@link edu.cmu.cs.fluid.mvc.Model#ELLIDED_NODES}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.DigraphModel#CHILDREN}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.SymmetricDigraphModel#PARENTS}
 * <li>{@link edu.cmu.cs.fluid.mvc.tree.ForestModel#LOCATION}
 * <li>{@link edu.cmu.cs.fluid.mvc.ConfigurableView#IS_HIDDEN}
 * <li>{@link edu.cmu.cs.fluid.mvc.ConfigurableView#PROXY_NODE}
 * <li>{@link edu.cmu.cs.fluid.mvc.ConfigurableView#IS_PROXY}
 * <li>{@link ConfigurableForestView#IS_EXPANDED}
 * <li>{@link ConfigurableForestView#RENDER_AS_PARENT}
 * <li>{@link SyntaxForestModel#OPERATOR}
 * </ul>
 */
public interface ConfigurableSyntaxForestView
extends ConfigurableForestView, SyntaxForestToSyntaxForestStatefulView
{
  // Model will/must already be locked when this is called
  public void setEllipsisAt( IRNode ellipsis, IRNode parent, IRLocation loc, Set<IRNode> nodes );
  
  
  
  /**
   * Factory interface for creating instances of
   * {@link ConfigurableSyntaxForestView}.
   */
  public static interface Factory
  {
    public ConfigurableSyntaxForestView create(
      String name, SyntaxForestModel src, VisibilityModel vizModel,
      AttributeInheritancePolicy aip, ForestProxyAttributePolicy pp,
      ForestEllipsisPolicy ellipsisPolicy, 
      boolean expFlat, boolean expPath )
    throws SlotAlreadyRegisteredException;
  }
}



