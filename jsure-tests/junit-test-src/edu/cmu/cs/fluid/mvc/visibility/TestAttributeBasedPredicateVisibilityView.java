/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/visibility/TestAttributeBasedPredicateVisibilityView.java,v 1.8 2006/03/30 16:20:26 chance Exp $
 *
 * TestAttributeBasedPredicateVisibilityView.java
 * Created on March 20, 2002, 10:25 AM
 */

package edu.cmu.cs.fluid.mvc.visibility;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import edu.cmu.cs.fluid.mvc.SimpleModelRenderer;
import edu.cmu.cs.fluid.mvc.attr.AttributeModel;
import edu.cmu.cs.fluid.mvc.attr.SimpleAttributeViewFactory;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.mvc.predicate.SimplePredicateViewFactory;
import edu.cmu.cs.fluid.mvc.sequence.LabeledSequence;
import edu.cmu.cs.fluid.mvc.sequence.SimpleLabeledSequenceFactory;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;

/**
 * Program to test {@link AttributeBasedPredicateVisibilityView}s.
 *
 * @author Aaron Greenhouse
 */
public class TestAttributeBasedPredicateVisibilityView
{
  private final SlotInfo<Boolean> isDisplayedSI; 
  private final AttributeBasedPredicateVisibilityView vizModel;
  
  @SuppressWarnings("unchecked")
private TestAttributeBasedPredicateVisibilityView()
  throws SlotAlreadyRegisteredException
  {
    /* Model from which we will obtain attribute and predicate models. */
    final LabeledSequence seq =
      SimpleLabeledSequenceFactory.mutablePrototype.create(
        "My Test Sequence", SimpleSlotFactory.prototype );

    /* Init PredicateModel */
    final PredicateModel predModel = 
      SimplePredicateViewFactory.prototype.create( "Predicate Model", seq );
    
    /* Init AttributeModel */
    final AttributeModel attrModel = 
      SimpleAttributeViewFactory.prototype.create( "Attribute Model", seq );
    
    /* Init VisibilityModel */
    vizModel =
      AttributeBasedPredicateVisibilityViewFactory.prototype.create(
        "Viz Model", attrModel, predModel );

    isDisplayedSI = vizModel.getNodeAttribute(
                    AttributeBasedPredicateVisibilityView.IS_DISPLAYED );

    final JMenu isVisibleMenu = new JMenu( "Set isDisplayed attr" );
    final JMenu setVisibleMenu = new JMenu( "Invoke setVisible" );
    final Iterator<IRNode> nodes = attrModel.getNodes();
    while( nodes.hasNext() ) {
      final IRNode node = nodes.next();
      if( attrModel.isNodeAttr( node ) ) {
        final String attrName = attrModel.getName( node );
        isVisibleMenu.add( new SetIsDisplayedAction( attrName, node, Boolean.TRUE ) );
        isVisibleMenu.add( new SetIsDisplayedAction( attrName, node, Boolean.FALSE ) );
        setVisibleMenu.add( new SetDisplayedAction( attrName, node, true ) );
        setVisibleMenu.add( new SetDisplayedAction( attrName, node, false ) );
      }
    }
    
    final JMenuBar menuBar = new JMenuBar();
    menuBar.add( isVisibleMenu );
    menuBar.add( setVisibleMenu );
    
    final JFrame frame = new SimpleModelRenderer( vizModel );
    frame.setJMenuBar( menuBar );

    frame.setVisible( true );
  }
  
  public static void main( final String[] args )
  throws SlotAlreadyRegisteredException
  {
    new TestAttributeBasedPredicateVisibilityView();
  }
  
  
  
  private final class SetIsDisplayedAction
  extends AbstractAction
  {
    private final IRNode node;
    private Boolean value;
    
    public SetIsDisplayedAction( final String attr, final IRNode n, final Boolean v )
    {
      super( "[" + attr + "].isDisplayed <- " + v );
      node = n;
      value = v;
    }
    @Override
    public void actionPerformed( final ActionEvent actionEvent )
    {
      node.setSlotValue( isDisplayedSI, value );
    }    
  }



  private final class SetDisplayedAction
  extends AbstractAction
  {
    private final IRNode node;
    private boolean value;
    
    public SetDisplayedAction( final String attr, final IRNode n, final boolean v )
    {
      super( "setDisplayed(" + attr + ", " + v + ")" );
      node = n;
      value = v;
    }
    @Override
    public void actionPerformed( final ActionEvent actionEvent )
    {
      vizModel.setDisplayed( node, value );
    }    
  }
}
