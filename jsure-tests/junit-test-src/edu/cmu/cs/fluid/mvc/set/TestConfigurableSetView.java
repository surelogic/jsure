// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/TestConfigurableSetView.java,v 1.11 2007/01/12 18:53:29 chance Exp $

package edu.cmu.cs.fluid.mvc.set;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.Set;

import javax.swing.*;

import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.predicate.PredicateModel;
import edu.cmu.cs.fluid.mvc.predicate.SimplePredicateViewFactory;
import edu.cmu.cs.fluid.mvc.visibility.PredicateBasedVisibilityView;
import edu.cmu.cs.fluid.mvc.visibility.PredicateBasedVisibilityViewFactory;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SimpleExplicitSlotFactory;

public class TestConfigurableSetView
{
  public static void main( final String[] args )
  throws Exception
  {
    // Init source set
    final LabeledSet set = 
      LabeledSetFactory.prototype.create(
        "test set", SimpleExplicitSlotFactory.prototype );

    // Init PredicateModel
    final PredicateModel predModel = 
      SimplePredicateViewFactory.prototype.create( "Predicate Model", set );

    // Init Visibility Model
    final PredicateBasedVisibilityView visModel =
      PredicateBasedVisibilityViewFactory.prototype.create(
        "Visibility Model", set, predModel );

    // Init configurable view
    final ConfigurableSetView config = 
      ConfigurableSetViewFactory.prototype.create(
        "Configurable Set", set, visModel,
        SimpleProxySupportingAttributeInheritancePolicy.prototype,
        new ProxyPolicy( set ), true );

    // add some stuff to the source set model
    final JMenu setHiddenAttr = new JMenu( "Set Hidden Attribute" );
    final JMenu setHiddenMethod = new JMenu( "setHidden()" );
    for( int i = 0; i < 10; i++ ) {
      final IRNode node = new PlainIRNode();
      final String label = ">>> Item # " + i + " <<<";
      set.addNode( node );
      set.setLabel( node, label );
      setHiddenAttr.add( new SetHiddenAttrAction( config, label, node, true ) );
      setHiddenAttr.add( new SetHiddenAttrAction( config, label, node, false ) );
      setHiddenMethod.add( new SetHiddenMethodAction( config, label, node, true ) );
      setHiddenMethod.add( new SetHiddenMethodAction( config, label, node, false ) );
    }

    final JCheckBox showEllipsis = new JCheckBox( "Show Ellipsis", true );
    showEllipsis.addItemListener( new EllipsisChangedListener( config ) );
    
    final JMenuBar menuBar = new JMenuBar();
    menuBar.add( setHiddenAttr );
    menuBar.add( setHiddenMethod );
    
    final JFrame frame = new JFrame( "Configurable Set View Test" );
    frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    final Container contentPane = frame.getContentPane();
    contentPane.setLayout( new BorderLayout() );
    contentPane.add( new SimpleRenderer( config ), BorderLayout.CENTER );
    contentPane.add( showEllipsis, BorderLayout.NORTH );
    contentPane.add(
      new JScrollPane( new ProxyNodePanel( config ) ), BorderLayout.SOUTH );
    
    
    frame.setJMenuBar( menuBar );
    frame.setSize( 640, 640 );
    frame.validate();
    frame.setVisible( true );
  }
  
  
  
  private static final class SetHiddenMethodAction
  extends AbstractAction
  {
    private final ConfigurableView model;
    private final IRNode node;
    private boolean value;
    
    public SetHiddenMethodAction( 
      final ConfigurableSetView csv, final String label,
      final IRNode n, final boolean v )
    {
      super( (v ? "Hide " : "Show ") + "\"" + label + "\"" );
      model = csv;
      node = n;
      value = v;
    }
    
    @Override
    public void actionPerformed( final ActionEvent actionEvent )
    {
      model.setHidden( node, value );
    }    
  }
  
  
  
  private static final class SetHiddenAttrAction
  extends AbstractAction
  {
    private final Model model;
    private final IRNode node;
    private Boolean value;
    
    public SetHiddenAttrAction( 
      final ConfigurableSetView csv, final String label,
      final IRNode n, final boolean v )
    {
      super( (v ? "Hide " : "Show ") + "\"" + label + "\"" );
      model = csv;
      node = n;
      value = v ? Boolean.TRUE : Boolean.FALSE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void actionPerformed( final ActionEvent actionEvent )
    {
      node.setSlotValue(
        model.getNodeAttribute( ConfigurableView.IS_HIDDEN ), value );
    }    
  }

  
  
  private static final class EllipsisChangedListener
  implements ItemListener
  {
    private final ConfigurableSetView csv;
    
    public EllipsisChangedListener( final ConfigurableSetView csv ) 
    {
      this.csv = csv;
    }
    
    @Override
    public void itemStateChanged( final ItemEvent e )
    {
      csv.setSetEllipsisPolicy( e.getStateChange() == ItemEvent.SELECTED );
    }
  }
  
  
  
  private static final class ProxyPolicy
  implements ProxyAttributePolicy
  {
    final Model srcModel;
    
    public ProxyPolicy( final Model src ) 
    {
      srcModel = src;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AVPair[] attributesFor( final Model model, final Set skippedNodes )
    {
      final StringBuilder sb = new StringBuilder( "ellipsis[" );
      for( final Iterator i = skippedNodes.iterator(); i.hasNext(); ) {
        final IRNode node = (IRNode) i.next();        
        sb.append(
          node.getSlotValue( srcModel.getNodeAttribute( LabeledSet.LABEL ) ) );
        if( i.hasNext() ) sb.append( ", " );
      }
      sb.append( ']' );
      return new AVPair[] { new AVPair( LabeledSet.LABEL, sb.toString() ) };
    }
  }
}

