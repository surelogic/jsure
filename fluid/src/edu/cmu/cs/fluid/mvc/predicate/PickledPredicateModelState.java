// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/predicate/PickledPredicateModelState.java,v 1.19 2007/07/10 22:16:39 aarong Exp $

package edu.cmu.cs.fluid.mvc.predicate;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.sequence.SequenceModel;
import edu.cmu.cs.fluid.mvc.set.SetModel;
import edu.cmu.cs.fluid.ir.*;

public class PickledPredicateModelState
{
  /** The pickled state */
  private final PredState[] state;

  /** The node of the model the state is from. */
  private final IRNode modelNode;

  

  //===========================================================
  //== Constructor
  //===========================================================

  /**
   * Create a state pickle for the given attribute model.
   * The model is considered to be an attribute model if it 
   * has the attributes:
   * {@link PredicateModel#ATTR_NODE},
   * {@link PredicateModel#PREDICATE},
   * {@link PredicateModel#IS_VISIBLE},
   * {@link PredicateModel#IS_STYLED},
   * {@link PredicateModel#ATTRIBUTE},
   * {@link PredicateModel#PREDICATES_OF},
   * {@link SequenceModel#SIZE},
   * {@link SequenceModel#LOCATION}, and
   * {@link SequenceModel#INDEX}.
   * @exception IllegalArgumentException Thrown if <code>model</code>
   * does not have all of the above attributes.
   */
  @SuppressWarnings("unchecked")
  public PickledPredicateModelState( final Model model )
  {
    final boolean isPredicateModel =
         model.isComponentAttribute( PredicateModel.PREDICATES_OF ) 
      && model.isComponentAttribute( SetModel.SIZE )
      && model.isNodeAttribute( PredicateModel.ATTR_NODE )
      && model.isNodeAttribute( PredicateModel.PREDICATE )
      && model.isNodeAttribute( PredicateModel.IS_VISIBLE )
      && model.isNodeAttribute( PredicateModel.IS_STYLED )
      && model.isNodeAttribute( PredicateModel.ATTRIBUTE )
      && model.isNodeAttribute( SequenceModel.LOCATION )
      && model.isNodeAttribute( SequenceModel.INDEX );
    
    if( !isPredicateModel ) {
      throw new IllegalArgumentException(   "Model \"" + model.getName()
                                          + "\" is not an PredicateModel" );
    }

    // Set the model node
    modelNode = model.getNode();

    // process the state
    final SlotInfo<IREnumeratedType.Element> isVisible = 
      model.getNodeAttribute( PredicateModel.IS_VISIBLE );
    final SlotInfo<Boolean> isStyled = 
      model.getNodeAttribute( PredicateModel.IS_STYLED );
    final Integer sizeInt = 
      (Integer)model.getCompAttribute( SetModel.SIZE ).getValue();

    state = new PredState[sizeInt.intValue()];
    
    // Because this is a sequence model, the nodes will be
    // in order.
    final Iterator nodes = model.getNodes();
    int count = 0;
    while( nodes.hasNext() ) {
      final IRNode node = (IRNode)nodes.next();
      final IREnumeratedType.Element elt = node.getSlotValue( isVisible );
      final Boolean sty = node.getSlotValue( isStyled );
      state[count] = new PredState( node, elt, sty );
      count += 1;
    }
  }

  private PickledPredicateModelState( final IRNode model, final PredState[] attrs )
  {
    modelNode = model;
    state = attrs;
  }



  //===========================================================
  //== Inner class for returning individual attribute state
  //===========================================================

  public static class PredState
  {
    public final IRNode predNode;
    public final IREnumeratedType.Element isVisible;
    public final Boolean isStyled;

    public PredState( final IRNode n, final IREnumeratedType.Element elt,
                      final Boolean sty )
    {
      predNode = n;
      isVisible = elt;
      isStyled = sty;
    }

    @Override
    public String toString()
    {
      return (predNode + "[" + isVisible + "," + isStyled + "]");
    }
  }



  //===========================================================
  //== Inner class for iterator
  //===========================================================

  private static class AttrIterator
  extends AbstractRemovelessIterator<PredState>
  {
    private final PredState[] state;
    private int current;
    private boolean done;

    protected AttrIterator( final PredState[] s )
    {
      state = s;
      current = 0;
      done = (current >= state.length);
    }

    @Override
    public boolean hasNext()
    {
      return !done;
    }

    @Override
    public PredState next()
    {
      if( done ) throw new NoSuchElementException();

      final PredState next = state[current];  
      current += 1;
      done = (current == state.length);
      return next;
    }
  }



  //===========================================================
  //== Getters
  //===========================================================

  /**
   * Get the IRNode of the PredicateModel that generated the pickle.
   */
  public IRNode getModel()
  {
    return modelNode;
  }

  /**
   * Get an iterator over the attributes of the state.
   * Returns an iterator over {@link PredState} instances.
   */
  public Iterator<PredState> getAttributes()
  {
    return new AttrIterator( state );
  }

  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder();
    final Iterator<PredState> iter = getAttributes();
    while( iter.hasNext() ) {
      final Object next = iter.next();
      buf.append( next.toString() );
      if( iter.hasNext() ) buf.append( "::" );
    }
    return buf.toString();
  }



  //===========================================================
  //== Methods for persisting the state
  //===========================================================

  public void writeValue( final IROutput out ) 
  throws IOException
  {
    out.writeNode( modelNode );
    out.writeInt( state.length );
    for( int i = 0; i < state.length; i++ ) {
      final PredState as = state[i];
      out.writeNode( as.predNode );
      as.isVisible.getType().writeValue( as.isVisible, out );
      out.writeBoolean( as.isStyled.booleanValue() );
    }
  }

  public static PickledPredicateModelState readValue( final IRInput in )
  throws IOException
  {
    final IREnumeratedType visEnum = IREnumeratedType.getIterator( PredicateModel.VISIBLE_ENUM );
    final IRNode modelNode = in.readNode();
    final int size = in.readInt();
    final PredState[] state = new PredState[size];
    for( int i = 0; i < size; i++ ) {
      final IRNode predNode = in.readNode();
      final IREnumeratedType.Element isVisible = visEnum.readValue( in );
      final boolean isStyled = in.readBoolean();
      state[i] = new PredState( predNode, isVisible,
                                (isStyled ? Boolean.TRUE : Boolean.FALSE) );
    }

    return new PickledPredicateModelState( modelNode, state );
  }
}
