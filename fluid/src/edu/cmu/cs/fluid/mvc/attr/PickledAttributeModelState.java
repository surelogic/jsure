// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/attr/PickledAttributeModelState.java,v 1.11 2007/01/12 18:53:30 chance Exp $
package edu.cmu.cs.fluid.mvc.attr;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.cmu.cs.fluid.mvc.Model;
import edu.cmu.cs.fluid.mvc.set.SetModel;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.util.AbstractRemovelessIterator;

public class PickledAttributeModelState
{
  /** The pickled state */
  private final AttrState[] state;

  /** The node of the model the state is from. */
  private final IRNode modelNode;

  

  //===========================================================
  //== Constructor
  //===========================================================

  /**
   * Create a state pickle for the given attribute model.
   * The model is considered to be an attribute model if it 
   * has the attributes:
   * {@link AttributeModel#ATTR_NAME},
   * {@link AttributeModel#ATTR_LABEL},
   * {@link AttributeModel#ATTR_TYPE},
   * {@link AttributeModel#ATTR_KIND},
   * {@link AttributeModel#IS_MUTABLE},
   * {@link AttributeModel#IS_NODE_ATTR},
   * {@link AttributeModel#ATTRIBUTES_OF}, and
   * {@link SetModel#SIZE}.
   * @exception IllegalArgumentException Thrown if <code>model</code>
   * does not have all of the above attributes.
   */
  public PickledAttributeModelState( final Model model )
  {
    final boolean isAttributeModel =
         model.isComponentAttribute( AttributeModel.ATTRIBUTES_OF ) 
      && model.isComponentAttribute( SetModel.SIZE )
      && model.isNodeAttribute( AttributeModel.ATTR_NAME )
      && model.isNodeAttribute( AttributeModel.ATTR_LABEL )
      && model.isNodeAttribute( AttributeModel.ATTR_TYPE )
      && model.isNodeAttribute( AttributeModel.ATTR_KIND )
      && model.isNodeAttribute( AttributeModel.IS_MUTABLE )
      && model.isNodeAttribute( AttributeModel.IS_NODE_ATTR );
    
    if( !isAttributeModel ) {
      throw new IllegalArgumentException(   "Model \"" + model.getName()
                                          + "\" is not an AttributeModel" );
    }

    // Set the model node
    modelNode = model.getNode();

    // process the state
    final SlotInfo<String> attrLabel = 
      model.getNodeAttribute( AttributeModel.ATTR_LABEL ); 
    final Integer size =
      (Integer)model.getCompAttribute( SetModel.SIZE ).getValue();

    state = new AttrState[size.intValue()];
    
    final Iterator<IRNode> nodes = model.getNodes();
    int count = 0;
    while( nodes.hasNext() ) {
      final IRNode node = nodes.next();
      final String label = node.getSlotValue( attrLabel );
      state[count] = new AttrState( node, label );
      count++;
    }
  }

  private PickledAttributeModelState( final IRNode model,
				      final AttrState[] attrs )
  {
    modelNode = model;
    state = attrs;
  }



  //===========================================================
  //== Inner class for returning individual attribute state
  //===========================================================

  public static class AttrState
  {
    public final IRNode attrNode;
    public final String label;

    public AttrState( final IRNode n, final String label )
    {
      attrNode = n;
      this.label = label;
    }

    @Override
    public String toString()
    {
      return (attrNode + "[" + label + "]");
    }
  }



  //===========================================================
  //== Inner class for iterator
  //===========================================================

  private static class AttrIterator
  extends AbstractRemovelessIterator<AttrState>
  {
    private final AttrState[] state;
    private int current;
    private boolean done;

    protected AttrIterator( final AttrState[] s )
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
    public AttrState next()
    {
      if( done ) throw new NoSuchElementException();

      final AttrState next = state[current];  
      current += 1;
      done = (current == state.length);
      return next;
    }
  }



  //===========================================================
  //== Getters
  //===========================================================

  /**
   * Get the IRNode of the AttributeModel that generated the pickle.
   */
  public IRNode getModel()
  {
    return modelNode;
  }

  /**
   * Get an iterator over the attributes of the state.
   * Returns an iterator over {@link AttrState} instances.
   */
  public Iterator<AttrState> getAttributes()
  {
    return new AttrIterator( state );
  }

  @Override
  public String toString()
  {
    final StringBuilder buf = new StringBuilder();
    final Iterator<AttrState> iter = getAttributes();
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
      final AttrState as = state[i];
      out.writeNode( as.attrNode );
      out.writeUTF( as.label );
    }
  }

  public static PickledAttributeModelState readValue( final IRInput in )
  throws IOException
  {
    final IRNode modelNode = in.readNode();
    final int size = in.readInt();
    final AttrState[] state = new AttrState[size];
    for( int i = 0; i < size; i++ ) {
      final IRNode attrNode = in.readNode();
      final String label = in.readUTF();
      state[i] = new AttrState( attrNode, label );
    }

    return new PickledAttributeModelState( modelNode, state );
  }
}

