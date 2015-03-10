/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AbstractAttributeInheritancePolicy.java,v 1.7 2005/05/25 15:52:03 chance Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract implemenation of attribute inheritance policy.  Subclasses
 * implement the {@link #filterCompAttr} and {@link #filterNodeAttr}
 * methods to control the attribute inheritance.
 */
public abstract class AbstractAttributeInheritancePolicy
implements AttributeInheritancePolicy
{
  /**
   * Constant returnsed by {@link #filterCompAttr} and {@link #filterNodeAttr}
   * to indicate that an attribute should not be inherited.
   */
  protected static final HowToInherit DONT_INHERIT = null;
  
  /**
   * Determine if a model-level attribute should be inherited, and if
   * so return a description of how to inherit that attribute.
   * @param from The model from which the attribute originates.
   * @param attr The attribute to possibly inherit.
   * @return {@link #DONT_INHERIT} if the attribute should not be inherited;
   *         a description of how to inherit the attribute otherwise.
   */
  protected abstract HowToInherit filterCompAttr( Model from, String attr );
  
  /**
   * Determine if a node-level attribute should be inherited, and if
   * so return a description of how to inherit that attribute.
   * @param from The model from which the attribute originates.
   * @param attr The attribute to possibly inherit.
   * @return {@link #DONT_INHERIT} if the attribute should not be inherited;
   *         a description of how to inherit the attribute otherwise.
   */
  protected abstract HowToInherit filterNodeAttr( Model from, String attr );
  
  
  
  /**
   * Generic implementation: loops over all the model-level attributes
   * of the given model, and invokes <code>filterCompAttr()</code> on each
   * one.
   * @param from The model whose attributes should possibly be inherited.
   */
  @Override
  public final HowToInherit[] compAttrsToInherit( final Model from )
  {
    final Iterator<String> attrs = from.getComponentAttributes();
    final List<HowToInherit> attrsToInherit = new LinkedList<HowToInherit>();
    
    while( attrs.hasNext() ) {
      final String attr = attrs.next();
      final HowToInherit how = filterCompAttr( from, attr );
      if( how != DONT_INHERIT ) attrsToInherit.add( how );
    }
    
    return attrsToInherit.toArray( AttributeInheritancePolicy.emptyArray );
  }
  
  
    
  /**
   * Generic implementation: loops over all the node-level attributes
   * of the given model, and invokes <code>filterNodeAttr()</code> on each
   * one.
   * @param from The model whose attributes should possibly be inherited.
   */
  @Override
  public final HowToInherit[] nodeAttrsToInherit( final Model from )
  {
    final Iterator<String> attrs = from.getNodeAttributes();
    final List<HowToInherit> attrsToInherit = new LinkedList<HowToInherit>();
    
    while( attrs.hasNext() ) {
      final String attr = attrs.next();
      final HowToInherit how = filterNodeAttr( from, attr );
      if( how != DONT_INHERIT ) attrsToInherit.add( how );
    }

    return attrsToInherit.toArray( AttributeInheritancePolicy.emptyArray );
  }
}
