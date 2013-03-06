/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ViewCore.java,v 1.25 2007/07/10 22:16:30 aarong Exp $ */
package edu.cmu.cs.fluid.mvc;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.cs.fluid.ir.*;

/**
 * Core implemenation of the <code>View</code> interface.
 *
 * <p>Adds the model-level attributes {@link View#VIEW_NAME} and
 * {@link View#SRC_MODELS}.
 *
 * @author Aaron Greenhouse
 */
public class ViewCore
extends AbstractCore
{
  //===========================================================
  //== Fields
  //===========================================================

  /** Storage for the SRC_MODELS attribute. */
  private final ComponentSlot<IRSequence<Model>> srcAttr;



  //===========================================================
  //== Constructor
  //===========================================================

  /**
   * Create a new ViewCore; only for use by factory objects
   * and subclasses.  To create a new instance use
   * a factory object.
   */
  protected ViewCore(
    final String name, final Model model, final Object lock,
    final AttributeManager manager )
  throws SlotAlreadyRegisteredException
  {
    super( model, lock, manager );

    // Init model attributes
    final ExplicitSlotFactory csf = ConstantExplicitSlotFactory.prototype;

    final ComponentSlot<String> nameAttr =
      new SimpleComponentSlot<String>( IRStringType.prototype, csf, name );
    attrManager.addCompAttribute(
      View.VIEW_NAME, Model.STRUCTURAL, nameAttr );

    srcAttr =
      new SimpleComponentSlot<IRSequence<Model>>( new IRSequenceType<Model>( ModelType.prototype ), csf );
    attrManager.addCompAttribute(
      View.SRC_MODELS, Model.STRUCTURAL, srcAttr );
  }
  


  //===========================================================
  //== Source Models convienence methods  
  //===========================================================

  public void setSourceModels( final IRSequence<Model> srcs )
  {
    srcAttr.setValue( srcs );
  }

  /** 
   * Get the models being viewed by the view.  Reads
   * the value of the {@link View#SRC_MODELS} attribute.
   * @return an Iterator over {@link edu.cmu.cs.fluid.mvc.Model}s.
   */
  @SuppressWarnings("unchecked")
  public Iterator<Model> getSourceModels()
  {
    final IRSequence seq =
      (IRSequence)attrManager.getCompAttribute( View.SRC_MODELS ).getValue();
    return seq.elements();
  }



  //===========================================================
  //== Query about the relationship between models
  //===========================================================

  /**
   * Query if the view is below a model in a
   * model&ndash;view chain.
   */
  public boolean downChainFrom( final Model model )
  {
    final List<Model> queue = new LinkedList<Model>();
    queue.add( this.partOf );
    
    boolean found = false;
    while( !queue.isEmpty() && !found ) {
      final View v = (View)queue.remove( 0 );
      final Iterator<Model> srcs = v.getSourceModels();
      while( srcs.hasNext() && !found ) {
        final Model m = srcs.next();
        if( m.equals( model ) ) found = true;
        else if( m instanceof View ) queue.add( m );
      }    
    }

    // Try to speed up gc
    queue.clear();
    return found;
  }



  //===========================================================
  //== ViewCore Factory Interfaces/Classes
  //===========================================================

  public static interface Factory 
  {
    public ViewCore create( String name, Model model,
                            Object structLock, AttributeManager manager )
    throws SlotAlreadyRegisteredException;
  }
  
  private static class StandardFactory
  implements Factory
  {
    @Override
    public ViewCore create( final String name, final Model model,
                             final Object structLock,
                             final AttributeManager manager )
    throws SlotAlreadyRegisteredException
    {
      return new ViewCore( name, model, structLock, manager );
    }
  }

  public static final Factory standardFactory = new StandardFactory();
}

