/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/AbstractModelToSequenceStatefulView.java,v 1.22 2007/07/10 22:16:30 aarong Exp $ */
package edu.cmu.cs.fluid.mvc.sequence;

import java.io.IOException;
import com.surelogic.common.util.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.*;

/**
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractModelToSequenceStatefulView
extends AbstractModelToModelStatefulView
{
  /** The SequenceModelCore delegate */
  protected final SequenceModelCore seqModCore;



  //===========================================================
  //== Constructor
  //===========================================================

  // Subclass must init SRC_MODELS attribute!
  // To prevent mutation of the INDEX and LOCATION attributes,
  // the SequenceModelCore should be passed false for the
  // isMutable parameter.
  public AbstractModelToSequenceStatefulView(
    final String name, final ModelCore.Factory mf, final ViewCore.Factory vf,
    final SequenceModelCore.Factory smf,
    final AttributeManager.Factory attrFactory, 
    final AttributeInheritanceManager.Factory inheritFactory )
  throws SlotAlreadyRegisteredException
  {
    super( name, mf, vf, mf.getFactory(), attrFactory, inheritFactory );
    seqModCore = smf.create( this, structLock, attrManager, name,
                             new SeqAttrChangedCallback() );
  }



  //===========================================================
  //== Callback
  //===========================================================

  // It would be nice if this could be defined in one place, 
  // rather than in both PureSequence and here.
  /**
   * Attribute changed callback that causes a SequenceModelEvent to be sent
   * when the {@link SequenceModel#INDEX} or
   * {@link SequenceModel#LOCATION} attributes are changed.
   */
  private class SeqAttrChangedCallback
  extends AbstractAttributeChangedCallback
  {
    @Override
    protected void attributeChangedImpl(
      final String attr, final IRNode node, final Object val )
    {
      SequenceModelEvent e = null;
      
      if( attr == SequenceModel.LOCATION ) {
        e = new SequenceModelEvent(
                  AbstractModelToSequenceStatefulView.this, SequenceModelEvent.NODE_MOVED,
                  node, (IRLocation)val );
      } else if( attr == SequenceModel.INDEX ) {
        e = new SequenceModelEvent(
                  AbstractModelToSequenceStatefulView.this, SequenceModelEvent.NODE_MOVED, node, 
                  seqModCore.location( ((Integer)val).intValue() ) );
      }
      
      if( e != null ) modelCore.fireModelEvent( e );
    }
  }



  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

  @Override
  public boolean isPresent( final IRNode node )
  {
    synchronized( structLock ) { return seqModCore.isPresent( node ); }
  }

  @Override
  public final void addNode( final IRNode node, final AVPair[] attrs )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  
  /**
   * Overridden to delegate to
   * {@link SequenceModelCore#setNodeAttributes}.
   */
  @Override
  public void setNodeAttributes( final IRNode n, final AVPair[] pairs )
  {
    synchronized( structLock ) {
      seqModCore.setNodeAttributes( n, pairs, modelCore );
    }
    modelCore.fireModelEvent( new AttributeValuesChangedEvent( this, n, pairs ) );
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End Model Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------



  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- Begin SequenceModel Portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------

   //===========================================================
  //== Node methods
  //===========================================================

  /**
   * Returns an iterator over the nodes in the order they
   * appear in the sequence.
   */
  @Override
  public final Iteratable<IRNode> getNodes()
  {
    synchronized( structLock ) { return seqModCore.getNodes(); }
  }



  //===========================================================
  //== Attribute convienence methods
  //===========================================================

  /**
   * Get the value of the {@link SequenceModel#LOCATION} attribute.
   */
  public final IRLocation getLocation( final IRNode node )
  {
    synchronized( structLock ) { return seqModCore.getLocation( node ); }
  }

  /**
   * Get the value of the {@link SequenceModel#INDEX} attribute.
   */
  public final int getIndex( final IRNode node )
  {
    synchronized( structLock ) { return seqModCore.getIndex( node ); }
  }


  // Inherit JavaDoc from Model interface
  @Override
  public String compValueToString( final String attr )
  throws UnknownAttributeException
  {
    synchronized( structLock ) { 
      return seqModCore.compValueToString( modelCore, attr );
    }
  }

  

  //===========================================================
  //== From IRSequence Interface
  //===========================================================

  @Override
  public IRState getParent() {
    return null; // TODO
  }
  
  // Inherit java doc for IRSequence 
  public final int size()
  {
    synchronized( structLock ) { return seqModCore.size(); }
  }

  // Inherit java doc for IRSequence 
  public final boolean isVariable()
  {
    synchronized( structLock ) { return seqModCore.isVariable(); }
  }

  // Inherit java doc for IRSequence 
  public final boolean hasElements() {
    synchronized( structLock ) { return seqModCore.hasElements(); }
  }

  // Inherit java doc for IRSequence 
  public final Iteratable<IRNode> elements() {
    synchronized( structLock ) { return seqModCore.elements(); }
  }

  // Inherit java doc for IRSequence 
  public final boolean validAt( final int i ) {
    synchronized( structLock ) { return seqModCore.validAt( i ); }
  }

  // Inherit java doc for IRSequence 
  public final boolean validAt(IRLocation loc) {
    synchronized( structLock ) { return seqModCore.validAt(loc); }
  }

  // Inherit java doc for IRSequence 
  public final IRNode elementAt( final int i ) {
    synchronized( structLock ) { return seqModCore.elementAt( i ); }
  }

  // Inherit java doc for IRSequence 
  public final IRNode elementAt( final IRLocation loc ) {
    synchronized( structLock ) { return seqModCore.elementAt(loc); }
  }

  // Inherit java doc for IRSequence 
  public final void setElementAt( final IRNode element, final IRLocation loc )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  // Inherit java doc for IRSequence 
  public final void setElementAt( final IRNode element, final int loc )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  // Inherit java doc for IRSequence 
  public final IRLocation insertElement( final IRNode element )    
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  
  // Inherit java doc for IRSequence 
  public final IRLocation appendElement( final IRNode element )    
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  
  // Inherit java doc for IRSequence 
  public final IRLocation insertElementBefore( final IRNode element, final IRLocation loc )    
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  
  // Inherit java doc for IRSequence 
  public final IRLocation insertElementAfter( final IRNode element, final IRLocation loc )    
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  
  // Inherit java doc for IRSequence 
  public final void removeElementAt( final IRLocation i ) {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
    
  // Inherit java doc for IRSequence 
  public final IRLocation location( final int i ) {
    synchronized( structLock ) { return seqModCore.location(i); }
  }

  // Inherit java doc for IRSequence 
  public final int locationIndex( final IRLocation loc ) {
    synchronized( structLock ) { return seqModCore.locationIndex(loc); }
  }

  // Inherit java doc for IRSequence 
  public final IRLocation firstLocation() {
    synchronized( structLock ) { return seqModCore.firstLocation(); }
  }

  // Inherit java doc for IRSequence 
  public final IRLocation lastLocation() {
    synchronized( structLock ) { return seqModCore.lastLocation(); }
  }

  // Inherit java doc for IRSequence 
  public final IRLocation nextLocation( final IRLocation loc ) {
    synchronized( structLock ) { return seqModCore.nextLocation(loc); }
  }
  
  // Inherit java doc for IRSequence 
  public final IRLocation prevLocation( final IRLocation loc ) {
    synchronized( structLock ) { return seqModCore.prevLocation(loc); }
  }

  // Inherit java doc for IRSequence 
  public final int compareLocations( final IRLocation loc1, final IRLocation loc2 ) {
    synchronized( structLock ) { 
      return seqModCore.compareLocations(loc1,loc2);
    }
  }

  public final void writeValue( final IROutput out)
  throws IOException
  {
    synchronized( structLock ) { seqModCore.writeValue( out ); }
  }

  public final void writeContents( final IRCompoundType t, final IROutput out)
  throws IOException
  {
    synchronized( structLock ) { seqModCore.writeContents( t, out ); }
  }

  public final void readContents( final IRCompoundType t, final IRInput in )
  throws IOException
  {
    synchronized( structLock ) { seqModCore.readContents( t, in ); }
  }

  public final boolean isChanged()
  {
    synchronized( structLock ) { return seqModCore.isChanged(); }
  }

  public final void writeChangedContents( final IRCompoundType t, final IROutput out )
  throws IOException
  {
    synchronized( structLock ) { seqModCore.writeChangedContents( t, out ); }
  }

  public final void readChangedContents( final IRCompoundType t, final IRInput in )
  throws IOException
  {
    synchronized( structLock ) { seqModCore.readChangedContents( t, in ); }
  }

  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------
  //-- End SequenceModel portion
  //-----------------------------------------------------------------------
  //-----------------------------------------------------------------------



  //===========================================================
  //== SetModel methods
  //===========================================================

  public void addNode( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  
  @Override
  public void removeNode( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
}

