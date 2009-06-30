package edu.cmu.cs.fluid.mvc.sequence;

import java.util.Set;

import edu.cmu.cs.fluid.mvc.AttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.ConfigurableView;
import edu.cmu.cs.fluid.mvc.ProxyAttributePolicy;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.ir.*;

/**
 * A view of an <em>unversioned</em> sequence that allows for 
 * nodes to be ellided.  The exported model can contain nodes
 * for which {@link #isEllipsis} is <code>true</code>.
 *
 * @author Aaron Greenhouse
 */
public interface ConfigurableSequenceView
extends ConfigurableView, SequenceToSequenceStatefulView
{
  /**
   * Attribute containing the ellipsis policy that should be used.
   * The value is of type {@link SequenceEllipsisPolicyType} and
   * is mutable.
   */
  public static final String ELLIPSIS_POLICY =
    "ConfigurableSequenceView.ELLIPSIS_POLICY";

  
  
  /**
   * Set the ellipsis policy.
   */
  public void setSequenceEllipsisPolicy( SequenceEllipsisPolicy p );

  /**
   * Get the ellipsis policy
   */
  public SequenceEllipsisPolicy getSequenceEllipsisPolicy();

  
  
  /**
   * Called by the {@link SequenceEllipsisPolicy} to insert
   * an ellipsis.
   * @param pos The index before which the ellipsis should be placed.
   * @param nodes The set of nodes that the ellipsis is replacing.
   */
  // Called by ellipsis policy, which is called by the Rebuilder,
  // running in the monitor.
  public void insertEllipsisBefore( IRLocation pos, Set<IRNode> nodes );

  /**
   * Called by the {@link SequenceEllipsisPolicy} to insert
   * an ellipsis.
   * @param pos The index after which the ellipsis should be placed.
   * @param nodes The set of nodes that the ellipsis is replacing.
   */
  // Called by ellipsis policy, which is called by the Rebuilder,
  // running in the monitor.
  public void insertEllipsisAfter( IRLocation pos, Set<IRNode> nodes );

  
  
  public static interface Factory
  {
    public ConfigurableSequenceView create(
      String name, SequenceModel src, VisibilityModel vizModel,
      AttributeInheritancePolicy attrPolicy, ProxyAttributePolicy proxyPolicy )
    throws SlotAlreadyRegisteredException;
  }
}