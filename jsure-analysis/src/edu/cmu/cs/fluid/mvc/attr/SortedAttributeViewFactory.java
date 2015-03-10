/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/SortedAttributeViewFactory.java,v 1.10 2006/03/29 19:54:51 chance Exp $
 *
 * SortedAttributeViewFactory.java
 * Created on March 7, 2002, 4:10 PM
 */

package edu.cmu.cs.fluid.mvc.attr;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.sequence.SequenceModelCore;
import edu.cmu.cs.fluid.util.StringSet;
import edu.cmu.cs.fluid.ir.*;

/**
 * Factory for creating instances of SortedAttributeView.  Models returned by
 * the factory implement only the minimum requirements of
 * {@link SortedAttributeView}.
 * 
 * <p>The class uses singleton patterns, and thus has a private
 * constructor.  Clients should use the {@link #immutablePrototype} or
 * {@link #mutSrcPrototype} fields to access the only instances of
 * this class.
 *
 * @author Aaron Greenhouse
 */
public final class SortedAttributeViewFactory
implements SortedAttributeView.Factory
{
  /**
   * Prototype factory that creates views that inherit the 
   * {@link AttributeModel} node attributes as 
   * {@link AttributeInheritanceManager#IMMUTABLE}.
   */
  public static final SortedAttributeView.Factory immutablePrototype =
    new SortedAttributeViewFactory( false );

  /**
   * Prototype factory that creates views that inherit the 
   * {@link AttributeModel} node attributes as 
   * {@link AttributeInheritanceManager#MUTABLE_SOURCE}.
   */
  public static final SortedAttributeView.Factory mutSrcPrototype = 
    new SortedAttributeViewFactory( true );


  
  /**
   * Whether the {@link AttributeModel} node attributes will inherited as 
   * {@link AttributeInheritanceManager#IMMUTABLE} (or 
   * {@link AttributeInheritanceManager#MUTABLE_SOURCE}).  Only
   * interesting when the instance is being used as a factory for
   * SortedViews.
   */
  private final boolean isMutable;
  
  
  
  public SortedAttributeViewFactory( final boolean mut )
  {
    isMutable = mut;
  }

  
  
  @Override
  public SortedAttributeView create(
    final String name, final AttributeModel srcModel, final String attr, 
    final boolean isAscending, final AttributeInheritancePolicy policy )
  throws SlotAlreadyRegisteredException
  {
    IRSequence<IRNode> seq = SimpleSlotFactory.prototype.newSequence(~0);
    return new SortedAttributeViewImpl(
                 name, srcModel, ModelCore.simpleFactory,
                 ViewCore.standardFactory, 
                 new SequenceModelCore.StandardFactory(
                       seq,
                       SimpleSlotFactory.prototype, false ),
                 new AttrAttrsForcingPolicy( srcModel, policy, isMutable ), 
                 attr, isAscending );
  }


  
  /**
   * Attribute policy that forces the required AttributeModel attributes
   * to be inherited.  The 
   * {@link AttributeModel#ATTR_LABEL} attribute is either inherited as
   * {@link AttributeInheritanceManager#IMMUTABLE} or 
   * {@link AttributeInheritanceManager#MUTABLE_SOURCE}.  All the other
   * AttributeModel attributes are inherited as 
   * {@link AttributeInheritanceManager#IMMUTABLE}.
   */
  private static final class AttrAttrsForcingPolicy
  implements AttributeInheritancePolicy
  {
    /**
     * The attribute model to inherit the attribute attributes from.
     */
    private final AttributeModel attrModel;
    
    /**
     * The policy this policy is modifying.
     */
    private final AttributeInheritancePolicy policy;
    
    /**
     * Whether the attribute {@link AttributeModel#ATTR_LABEL} should be 
     * {@link AttributeInheritanceManager#MUTABLE_SOURCE}.
     */
    private final boolean isMutable;
    
    public AttrAttrsForcingPolicy(
      final AttributeModel src, final AttributeInheritancePolicy p,
      final boolean mutable )
    {
      attrModel = src;
      policy = p;
      isMutable = mutable;
    }

    

    @Override
    public HowToInherit[] compAttrsToInherit( final Model from )
    {

      final HowToInherit[] attrs = policy.compAttrsToInherit( from );
      if( from == attrModel ) {
        final List<HowToInherit> attrsToInherit = new LinkedList<HowToInherit>();
        attrsToInherit.add(
          new HowToInherit(
                AttributeModel.ATTRIBUTES_OF, AttributeModel.ATTRIBUTES_OF,
                AttributeInheritanceManager.IMMUTABLE, Model.INFORMATIONAL ) );
        for( int i = 0; i < attrs.length; i++ ) {
          if( attrs[i].attr != AttributeModel.ATTRIBUTES_OF ) {
            attrsToInherit.add( attrs[i] );
          }
        }
        return attrsToInherit.toArray( AttributeInheritancePolicy.emptyArray );
      } else {
        return attrs;
      }
    }
  
    @Override
    public HowToInherit[] nodeAttrsToInherit( final Model from )
    {

      final HowToInherit[] attrs = policy.nodeAttrsToInherit( from );
      if( from == attrModel ) {
        final List<HowToInherit> attrsToInherit = new LinkedList<HowToInherit>();
        final Object mode =   isMutable
                            ? AttributeInheritanceManager.MUTABLE_SOURCE
                            : AttributeInheritanceManager.IMMUTABLE;
        
        attrsToInherit.add(
          new HowToInherit(
                AttributeModel.ATTR_NAME, AttributeModel.ATTR_NAME,
                AttributeInheritanceManager.IMMUTABLE, Model.INFORMATIONAL ) );
        attrsToInherit.add(
          new HowToInherit(
                AttributeModel.ATTR_KIND, AttributeModel.ATTR_KIND,
                AttributeInheritanceManager.IMMUTABLE, Model.INFORMATIONAL ) );
        attrsToInherit.add(
          new HowToInherit(
                AttributeModel.ATTR_TYPE, AttributeModel.ATTR_TYPE,
                AttributeInheritanceManager.IMMUTABLE, Model.INFORMATIONAL ) );
        attrsToInherit.add(
          new HowToInherit(
                AttributeModel.IS_MUTABLE, AttributeModel.IS_MUTABLE,
                AttributeInheritanceManager.IMMUTABLE, Model.INFORMATIONAL ) );
        attrsToInherit.add(
          new HowToInherit(
                AttributeModel.DOMAIN, AttributeModel.DOMAIN,
                AttributeInheritanceManager.IMMUTABLE, Model.INFORMATIONAL ) );
        attrsToInherit.add(
          new HowToInherit(
                AttributeModel.IS_NODE_ATTR, AttributeModel.IS_NODE_ATTR,
                AttributeInheritanceManager.IMMUTABLE, Model.INFORMATIONAL ) );

        attrsToInherit.add(
          new HowToInherit(
                AttributeModel.ATTR_LABEL, AttributeModel.ATTR_LABEL,
                mode, Model.INFORMATIONAL ) );

        for( int i = 0; i < attrs.length; i++ ) {
          if( !ATTR_ATTRIBUTES.contains( attrs[i].attr ) ) {
            attrsToInherit.add( attrs[i] );
          }
        }
        return attrsToInherit.toArray( AttributeInheritancePolicy.emptyArray );
      } else {
        return attrs;
      }
    }
  }
  
  /**
   * The set of node-level attributes that an AttributeModel must
   * have.
   */
  private static final Set ATTR_ATTRIBUTES =
    new StringSet( new String[] { AttributeModel.ATTR_NAME,
                                  AttributeModel.ATTR_KIND,
                                  AttributeModel.ATTR_TYPE,
                                  AttributeModel.IS_MUTABLE,
                                  AttributeModel.DOMAIN,
                                  AttributeModel.IS_NODE_ATTR,
                                  AttributeModel.ATTR_LABEL} );

}
