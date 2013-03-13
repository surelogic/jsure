/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/AbstractSequenceModel.java,v 1.13 2007/07/10 22:16:30 aarong Exp $
 *
 * AbstractSequenceModel.java
 * Created on March 1, 2002, 1:10 PM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import java.io.IOException;
import com.surelogic.common.util.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.ir.*;

/**
 * Abstract implementation of a sequence model. 
 *
 * @author Aaron Greenhouse
 */
public abstract class AbstractSequenceModel
extends AbstractModel
{
   /** Reference to the SequenceModelCore delegate. */
   protected final SequenceModelCore seqModCore;



  //===========================================================
  //== Callbacks
  //===========================================================

  // It would be nice if this could be defined in one place, 
  // rather than in both AbstractModelToSequenceStatefulView and here.
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
                  AbstractSequenceModel.this, SequenceModelEvent.NODE_MOVED,
                  node, (IRLocation)val );
      } else if( attr == SequenceModel.INDEX ) {
        e = new SequenceModelEvent(
                  AbstractSequenceModel.this, SequenceModelEvent.NODE_MOVED, node, 
                  seqModCore.location( ((Integer)val).intValue() ) );
      }
      
      if( e != null ) modelCore.fireModelEvent( e );
    }
  }



  //===========================================================
  //== Constructors
  //===========================================================

  protected AbstractSequenceModel(
    final String name, final ModelCore.Factory mf,
    final SequenceModelCore.Factory smf, final SlotFactory sf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, LocalAttributeManagerFactory.prototype, sf );
    seqModCore = smf.create( this, structLock, attrManager, name,
                             new SeqAttrChangedCallback() );
  }

  
  
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
    synchronized( structLock ) {
      return seqModCore.getNodes();
    }
  }

  /**
   * Add a node to the sequence.
   * If no value for the {@link SequenceModel#LOCATION} attribute is
   * specified, then the node is appended to the sequence.  Otherwise the
   * node is inserted before the given position.  An exception is thrown
   * if the location is not valid for the sequence.
   *
   * @param node The node to add to the sequence.
   * @param vals The attributes and their initial values.
   * @exception UnknownAttributeException 
   * Thrown when the node is rejected because a given attribute is unrecognized.
   * @exception IllegalArgumentException
   * Thrown when the node is rejected because some attribute values would
   * result in the model being in an illegal state.
   */
  @Override
  public void addNode( final IRNode node, final AVPair[] vals )
  {
    synchronized( structLock ) {
      seqModCore.addNode( node, vals );
    }
  }

  @Override
  public void removeNode( final IRNode node )
  {
    IRLocation i = null;
    synchronized( structLock ) {
      try {
        i = seqModCore.getLocation( node );
      } catch( SlotUndefinedException e ) {
        return;
      }
      seqModCore.removeElementAt( i );
    }
    modelCore.fireModelEvent(
      new SequenceModelEvent( this, SequenceModelEvent.NODE_REMOVED, i ) );
  }

  @Override
  public boolean isPresent( final IRNode node )
  {
    synchronized( structLock ) { return seqModCore.isPresent( node ); }
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



  //===========================================================
  //== IRSequence methods
  //===========================================================

  @Override
  public IRState getParent() {
    return null; // TODO
  }
  
  public final int size() {
    synchronized( structLock ) { return seqModCore.size(); }
  }

  public final boolean isVariable() {
    synchronized( structLock ) { return seqModCore.isVariable(); }
  }

  public final boolean hasElements() { 
    synchronized( structLock ) { return seqModCore.hasElements(); }
  }

  public final Iteratable<IRNode> elements() { 
    synchronized( structLock ) { return seqModCore.elements(); }
  }

  public final boolean validAt( final int i ) {
    synchronized( structLock ) { return seqModCore.validAt( i ); }
  }

  public final boolean validAt( final IRLocation loc ) { 
    synchronized( structLock ) { return seqModCore.validAt( loc ); }
  }
  
  public final IRNode elementAt( final int i ) { 
    synchronized( structLock ) { return seqModCore.elementAt( i ); }
  }

  public final IRNode elementAt( final IRLocation loc ) {
    synchronized( structLock ) { return seqModCore.elementAt( loc ); }
  }

  public final void setElementAt( final IRNode element, final int i ) {
    IRLocation where;
    synchronized( structLock ) { 
      where = seqModCore.location( i );
      seqModCore.setElementAt( element, i );
    }
    modelCore.fireModelEvent(
      new SequenceModelEvent(
            this, SequenceModelEvent.NODE_REPLACED, element, where ) );
  }

  public final void setElementAt( final IRNode element, final IRLocation loc ) {
    synchronized( structLock ) { seqModCore.setElementAt( element, loc ); }
    modelCore.fireModelEvent(
      new SequenceModelEvent(
            this, SequenceModelEvent.NODE_REPLACED, element, loc ) );
  }

  public final IRLocation insertElement( final IRNode element ) {
    IRLocation loc = null;
    synchronized( structLock ) { loc = seqModCore.insertElement( element ); }
    modelCore.fireModelEvent(
      new SequenceModelEvent(
            this, SequenceModelEvent.NODE_INSERTED, element, loc ) );
    return loc;
  }
  
  public final IRLocation appendElement( final IRNode element ) {
    IRLocation loc = null;
    synchronized( structLock ) { loc = seqModCore.appendElement( element ); }
    modelCore.fireModelEvent(
      new SequenceModelEvent(
            this, SequenceModelEvent.NODE_INSERTED, element, loc ) );
    return loc;
  }
  
  public final IRLocation insertElementBefore( final IRNode element, final IRLocation i ) {
    IRLocation loc = null;
    synchronized( structLock ) { loc = seqModCore.insertElementBefore( element, i ); }
    modelCore.fireModelEvent(
      new SequenceModelEvent(
            this, SequenceModelEvent.NODE_INSERTED, element, loc ) );
    return loc;
  }
  
  public final IRLocation insertElementAfter( final IRNode element, final IRLocation i ) {
    IRLocation loc = null;
    synchronized( structLock ) { loc = seqModCore.insertElementAfter( element, i ); }
    modelCore.fireModelEvent(
      new SequenceModelEvent(
            this, SequenceModelEvent.NODE_INSERTED, element, loc ) );
    return loc;
  }
  
  public final void removeElementAt( final IRLocation i ) {
    synchronized( structLock ) { seqModCore.removeElementAt( i ); }
    modelCore.fireModelEvent(
      new SequenceModelEvent( this, SequenceModelEvent.NODE_REMOVED, i ) );
  }
  
  public final IRLocation location( final int i ) {
    synchronized( structLock ) { return seqModCore.location( i ); }
  }
  
  public final int locationIndex( final IRLocation loc ) {
    synchronized( structLock ) { return seqModCore.locationIndex( loc ); }
  }
  
  public final IRLocation firstLocation() {
    synchronized( structLock ) { return seqModCore.firstLocation(); }
  }
  
  public final IRLocation lastLocation() {
    synchronized( structLock ) { return seqModCore.lastLocation(); }
  }
  
  public final IRLocation nextLocation( final IRLocation loc ) {
    synchronized( structLock ) { return seqModCore.nextLocation( loc ); }
  }
  
  public final IRLocation prevLocation( final IRLocation loc ) {
    synchronized( structLock ) { return seqModCore.prevLocation( loc ); }
  }

  public final int compareLocations( final IRLocation loc1, final IRLocation loc2 ) {
    synchronized( structLock ) {
      return seqModCore.compareLocations( loc1, loc2 );
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
  //== SetModel methods
  //===========================================================

  public void addNode( final IRNode node )
  {
    appendElement( node );
  }
}
