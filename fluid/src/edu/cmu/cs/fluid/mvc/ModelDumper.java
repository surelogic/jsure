// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/ModelDumper.java,v 1.16 2007/07/05 18:15:16 aarong Exp $

package edu.cmu.cs.fluid.mvc;

import java.io.PrintStream;
import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Class that dumps the contents of the model to a text stream when
 * ever the model breaks.
 */
public class ModelDumper extends ModelAdapter
{
  /** Name of the model dumper */
  protected String name;
  
  /** The model to dump. */
  protected final Model model;

  /** The stream to which the model is printed. */
  protected final PrintStream writer;

  /** the added listener */
  private ModelListener listener = null;

  /** Used to signal clients that a break has occured */
  private final Object notifier = new Object();
  
  /** count of events */
  private int eventCount = 0;
  
  /** Should RebuildEvents be ignored? */
  private final boolean ignoreRebuilds;
  
  
  
  //======================================================================

  /**
   * Create a model dumper that listens the given model.
   */
  public ModelDumper( final Model mod, final PrintStream w )
  {
    this( mod, w, true );
  }

  /**
   * Create a new model dumper
   * @param mod The model to dump.
   * @param w The Stream to dumpt it to.
   * @param add <code>true</code> if the dumper should listen to the
   * model and dump whenever the model breaks.
   */
  public ModelDumper( final Model mod, final PrintStream w, final boolean add )
  {
    this( mod, w, add, true );
  }

  /**
   * Create a new model dumper
   * @param mod The model to dump.
   * @param w The Stream to dumpt it to.
   * @param add <code>true</code> if the dumper should listen to the
   * model and dump whenever the model breaks.
   * @param ignore <code>true</code> if the dumper should ignore 
   *   {@link RebuildEvent}s.
   */
  public ModelDumper(
    final Model mod, final PrintStream w,
    final boolean add, final boolean ignore )
  {
    this( "<unnamed>", mod, w, add, ignore );
  }

  /**
   * Create a new named model dumper
   * @param n The name of the mode dumper.
   * @param mod The model to dump.
   * @param w The Stream to dumpt it to.
   * @param add <code>true</code> if the dumper should listen to the
   * model and dump whenever the model breaks.
   * @param ignore <code>true</code> if the dumper should ignore 
   *   {@link RebuildEvent}s.
   */
  public ModelDumper(
    final String n, final Model mod, final PrintStream w,
    final boolean add, final boolean ignore )
  {
    name = n;
    model = mod;
    writer = w;
    ignoreRebuilds = ignore;
    if( add ) {
      listener = new ThreadedModelAdapter( this );
      model.addModelListener( listener );
    }
  }

  /**
   * Set the name of the model dumper.
   */
  public void setName( final String n )
  {
    name = n;
  }
  
  /**
   * Dump the model.
   */
  public final void dumpModel()
  {
    dumpCompAttributes();
    writer.println();
    dumpModelStructure();
    writer.println( "===========================================\n" );
    
    synchronized( notifier ) {
      eventCount += 1;
      notifier.notifyAll();
    }
  }

  // for backward compatibility
  public final void dumpModel( final Model m )
  {
    dumpModel();
  }

  /**
   * Wait for the model to be dumped.
   */
  public void waitForBreak()
  throws InterruptedException
  {
    synchronized( notifier ) {
      final int current = eventCount;
      while( current == eventCount ) {
        notifier.wait();
      }
    }
  }
  
  //======================================================================

  /**
   * Walk the strcuture of the model and dump each node.
   * Subclasses specialized to specific kinds of models should
   * override this to account for what ever model-specific structure
   * exists.
   */
  protected void dumpModelStructure()
  {
    final Iterator nodes = model.getNodes();
    while( nodes.hasNext() ) {
      final IRNode node = (IRNode)nodes.next();
      writer.println( "*** Node: " + model.idNode( node ) + " ***" );
      dumpNodeAttributes( node );
    }
  }

  /**
   * Dump the component-level attributes.
   */
  protected void dumpCompAttributes()
  {
    final Iterator modelAttrs = model.getComponentAttributes();
    while( modelAttrs.hasNext() ) {
      final String attr = (String)modelAttrs.next();
      final ComponentSlot cs = model.getCompAttribute( attr );
      writer.print( "Component Attribute \"" + attr + "\" " );
      if( cs.isValid() ) {
	writer.println( "= " + model.compValueToString( attr ) );
      } else {
	writer.println( "is undefined" );
      }
    }
  }

  /**
   * Dump the attributes a given node.
   */
  protected void dumpNodeAttributes( final IRNode node )
  {
    dumpNodeAttributes( node, "" );
  }

  /**
   * The attributes of a given node, prefixing each line with a given
   * string.
   */
  protected void dumpNodeAttributes( final IRNode node, final String prefix )
  {
    final Iterator nodeAttrs = model.getNodeAttributes();
    while( nodeAttrs.hasNext() ) {
      final String attr = (String)nodeAttrs.next();
      final SlotInfo si = model.getNodeAttribute( attr );
      writer.print( prefix + "  " + attr  );
      if( node.valueExists( si ) ) {
        writer.println( " = " + model.nodeValueToString( node, attr ) );
      }
      else
        writer.println( " is Undefined");
    }
    
    try {
      final IRNode proxy =
        (IRNode) node.getSlotValue(model.getNodeAttribute(ConfigurableView.PROXY_NODE));
      if( proxy != null ) {
        writer.println( prefix + "*** PROXY NODE ATTRIBUTES ***" );
        dumpNodeAttributes( proxy, prefix + "**" );
      }
    } catch( UnknownAttributeException e ) {
      // no proxy node
    }
  }

  //======================================================================

  @Override
  public void breakView( final ModelEvent e )
  {  
    if( !(ignoreRebuilds && !e.shouldCauseRebuild()) ) {
      writer.println( "Model dumper \"" + name + "\" received event: " + e );
      dumpModel();
    }
  }
}

