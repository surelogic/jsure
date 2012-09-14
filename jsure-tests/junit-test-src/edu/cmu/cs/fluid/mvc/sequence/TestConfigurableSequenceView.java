// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/TestConfigurableSequenceView.java,v 1.19 2007/01/12 18:53:28 chance Exp $

package edu.cmu.cs.fluid.mvc.sequence;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
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
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;

public class TestConfigurableSequenceView
{
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
          node.getSlotValue(
            srcModel.getNodeAttribute( LabeledSequence.LABEL ) ) );
        if( i.hasNext() ) sb.append( ", " );
      }
      sb.append( ']' );
      return new AVPair[] { new AVPair( LabeledSequence.LABEL, sb.toString() ) };
    }
  }



  public static void main( final String[] args )
  throws Exception
  {
    final LabeledSequence seq =
      SimpleLabeledSequenceFactory.mutablePrototype.create(
        "My Test Sequence", SimpleSlotFactory.prototype );
    
    // Init PredicateModel
    final PredicateModel predModel = 
      SimplePredicateViewFactory.prototype.create( "Predicate Model", seq );

    // Init Visibility Model
    final PredicateBasedVisibilityView visModel =
      PredicateBasedVisibilityViewFactory.prototype.create(
        "Visibility Model", seq, predModel );

    // Init configurable view
    final ConfigurableSequenceView config = 
      ConfigurableSequenceViewFactory.prototype.create(
        "Configurable Sequence", seq, visModel,
        SimpleProxySupportingAttributeInheritancePolicy.prototype,
        new ProxyPolicy( seq ) );

    // add some stuff to the source set model
    final JMenu setHiddenAttr = new JMenu( "Set Hidden Attribute" );
    final JMenu setHiddenMethod = new JMenu( "setHidden()" );
    for( int i = 0; i < 10; i++ ) {
      final IRNode node = new PlainIRNode();
      final String label = ">>> Item # " + i + " <<<";
      seq.addNode(
        node, new AVPair[] { new AVPair( LabeledSequence.LABEL, label ) } );
      setHiddenAttr.add( new SetHiddenAttrAction( config, label, node, true ) );
      setHiddenAttr.add( new SetHiddenAttrAction( config, label, node, false ) );
      setHiddenMethod.add( new SetHiddenMethodAction( config, label, node, true ) );
      setHiddenMethod.add( new SetHiddenMethodAction( config, label, node, false ) );
    }

    final JMenu setEllipsisAttr = new JMenu( "Set Ellipsis Policy" );
    final JMenu setEllipsisMethod = new JMenu( "setEllipsisPolicy()" );
    setEllipsisAttr.add(
      new SetEllipsisAttrAction( "No ellipsis", config,
                                 NoEllipsisSequenceEllipsisPolicy.prototype ) );
    setEllipsisMethod.add(
      new SetEllipsisMethodAction( "No ellipsis", config,
                                   NoEllipsisSequenceEllipsisPolicy.prototype ) );
    
    SequenceEllipsisPolicy policy;
    policy = new SingleEllipsisSequenceEllipsisPolicy( config, true );
    setEllipsisAttr.add( new SetEllipsisAttrAction( "Top", config, policy ) );
    setEllipsisMethod.add( new SetEllipsisMethodAction( "Top", config, policy ) );
    policy = new SingleEllipsisSequenceEllipsisPolicy( config, false );
    setEllipsisAttr.add( new SetEllipsisAttrAction( "Bottom", config, policy ) );
    setEllipsisMethod.add( new SetEllipsisMethodAction( "Bottom", config, policy ) );
    policy = new MultipleEllipsisSequenceEllipsisPolicy( config );
    setEllipsisAttr.add( new SetEllipsisAttrAction( "Many", config, policy ) );
    setEllipsisMethod.add( new SetEllipsisMethodAction( "Many", config, policy ) );
    
    final JMenuBar menuBar = new JMenuBar();
    menuBar.add( setHiddenAttr );
    menuBar.add( setHiddenMethod );
    menuBar.add( setEllipsisAttr );
    menuBar.add( setEllipsisMethod );
    
    final JFrame frame = new JFrame( "Configurable Set View Test" );
    frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    final Container contentPane = frame.getContentPane();
    contentPane.setLayout( new BorderLayout() );
    contentPane.add( new SimpleRenderer( config ), BorderLayout.CENTER );
    contentPane.add(
      new JScrollPane( new ProxyNodePanel( config ) ), BorderLayout.SOUTH );
    
    frame.setJMenuBar( menuBar );
    frame.setSize( 640, 640 );
    frame.validate();
    frame.setVisible( true );
  }
  
  
  
  private static final class SetEllipsisMethodAction
  extends AbstractAction
  {
    private final ConfigurableSequenceView model;
    private final SequenceEllipsisPolicy policy;
    
    public SetEllipsisMethodAction( final String label,
      final ConfigurableSequenceView m, final SequenceEllipsisPolicy p )
    {
      super( label );
      model = m;
      policy = p;
    }
    
    @Override
    public void actionPerformed( final ActionEvent e ) 
    {
      model.setSequenceEllipsisPolicy( policy );
    }
  }  
  
  
  private static final class SetEllipsisAttrAction
  extends AbstractAction
  {
    private final ConfigurableSequenceView model;
    private final SequenceEllipsisPolicy policy;
    
    public SetEllipsisAttrAction( final String label,
      final ConfigurableSequenceView m, final SequenceEllipsisPolicy p )
    {
      super( label );
      model = m;
      policy = p;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void actionPerformed( final ActionEvent e ) 
    {
      model.getCompAttribute(
        ConfigurableSequenceView.ELLIPSIS_POLICY ).setValue( policy );
    }
  }
  
  
  
  private static final class SetHiddenMethodAction
  extends AbstractAction
  {
    private final ConfigurableView model;
    private final IRNode node;
    private boolean value;
    
    public SetHiddenMethodAction( 
      final ConfigurableView csv, final String label,
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
      final ConfigurableView csv, final String label,
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
}

