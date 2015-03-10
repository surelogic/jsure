/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/SimplePredicateViewImpl.java,v 1.11 2007/07/05 18:15:22 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.predicate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.sequence.SequenceModelCore;
import edu.cmu.cs.fluid.ir.*;


/**
 * Minimal implementation of {@link SimplePredicateView}.
 * <em>Should probably do something about listening for
 * new predicates</em>.
 * @author Aaron Greenhouse
 */
final class SimplePredicateViewImpl
extends AbstractModelToPredicateStatefulView
implements SimplePredicateView
{
  /**
   * Map from attribute name to the <code>IRNode</code> that 
   * represents it in the sequence.  This is to insure that
   * the same node is used to represent an attribute after
   * a rebuild.
   */
  private final Map<String,IRNode> attrNodes;
  
  

  //===========================================================
  //== Constructor
  //===========================================================

  public SimplePredicateViewImpl(
    final String name, final Model src, final ModelCore.Factory mf,
    final ViewCore.Factory vf, final SequenceModelCore.Factory smf,
    final PredicateModelCore.Factory amf )
  throws SlotAlreadyRegisteredException
  {
    super( name, src, mf, vf, smf, amf, LocalAttributeManagerFactory.prototype,
           NullAttributeInheritanceManagerFactory.prototype );
    attrNodes = new HashMap<String,IRNode>();

    // need to get notification of new attributes some how

    // initialized the model
    rebuildModel();
    finalizeInitialization();
  }



  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin AbstractModelToModelStatefulView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  /**
   * Invoked when a new attribute is added to the source model.  In this 
   * case we trigger a rebuild when the new attribute is a node-level attribute.
   */
  @Override
  protected void attributeAddedToSource( 
    final Model src, final String attr, final boolean isNodeLevel )
  {
    if( isNodeLevel ) {
      signalBreak( new ModelEvent( src ) );
    }
  }
  
  @Override
  protected void rebuildModel( final List events )
  {
    // ought to protect against new attributes being added during 
    // a rebuild...

    synchronized( structLock ) {
      seqModCore.clearModel();

      final Iterator attrs = srcModel.getNodeAttributes();
      while( attrs.hasNext() ) {        
        final String attrName = (String)attrs.next();
        final SlotInfo si =     srcModel.getNodeAttribute( attrName );
        final IRNode attrNode = srcModel.getNodeAttrNode( attrName );
        final IRType type =     si.getType();
        
        if( type instanceof IRBooleanType ) {
          addBooleanAttribute( attrNode, attrName, si );
        } else if( type instanceof IREnumeratedType ) {
          addEnumeratedAttribute( attrNode, attrName, si, (IREnumeratedType)type );
        } else {
          addOtherAttribute( attrNode, attrName, si );
        }                
      }
    }
    modelCore.fireModelEvent( new ModelEvent( this ) );
  }

  private IRNode getAttrNode( final String attrKey )
  {
    IRNode node = attrNodes.get( attrKey );
    if( node == null ) {
      node = new MarkedIRNode("SimplePredView");
      attrNodes.put( attrKey, node );
    }
    return node;
  }

  private void addOtherAttribute(
    final IRNode attrNode, final String attrName, final SlotInfo si )
  {
    final IRNode node = getAttrNode( attrName );
    appendPredicateNode( node, attrNode, ConstantAttributePredicate.ALL, si );
    setLabelIfNone(node, attrName + "$ALL");
  }

  private void addBooleanAttribute(
    final IRNode attrNode, final String attrName, final SlotInfo si )
  {
    final String name1 = attrName + "$TRUE";
    final IRNode node1 = getAttrNode( name1 );
    appendPredicateNode( node1, attrNode, BooleanAttributePredicate.TRUE, si );
    setLabelIfNone(node1, name1);  
        
    final String name2 = attrName + "$FALSE";
    final IRNode node2 = getAttrNode( name2 );
    appendPredicateNode( node2, attrNode, BooleanAttributePredicate.FALSE, si );
    setLabelIfNone(node2, name2);    
  }

  private void addEnumeratedAttribute(
    final IRNode attrNode, final String attrName,
    final SlotInfo si, final IREnumeratedType enm )
  {
    for( int i = 0; i < enm.size(); i++ ) {
      final IREnumeratedType.Element elt = enm.getElement( i );
      final String name = attrName + "$" + elt;
      final IRNode node = getAttrNode( attrName + "$" + elt );
      appendPredicateNode(
        node, attrNode, new EnumeratedAttributePredicate( elt ), si );
      setLabelIfNone(node, name);
    }
  }

  private void setLabelIfNone(IRNode node, String label) {    
    if (getLabel(node) == null) {
      setLabel(node, label);
    }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End AbstractModelToModelStatefulView Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
}

