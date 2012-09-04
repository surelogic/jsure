// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/set/TestLabeledSet.java,v 1.7 2004/09/10 17:33:53 boyland Exp $

package edu.cmu.cs.fluid.mvc.set;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.SimpleModelRenderer;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SimpleExplicitSlotFactory;

public final class TestLabeledSet
{
  public static void main( final String[] args )
  throws Exception
  {
    final SetModel set =
      LabeledSetFactory.prototype.create(
        "test set", SimpleExplicitSlotFactory.prototype );

    final SimpleModelRenderer renderer = new SimpleModelRenderer( set );
    final JMenu removeMenu = new JMenu( "Remove" );
    final JMenuBar menuBar = new JMenuBar();
    menuBar.add( removeMenu );
    renderer.setJMenuBar( menuBar );
    
    final JPanel addPanel = new JPanel();
    final JButton addButton = new JButton( "Add" );
    final JTextField addField = new JTextField( 15 );
    addPanel.add( addButton );
    addPanel.add( addField );
    addButton.addActionListener( new AddAction( set, addField, removeMenu ) );

    renderer.getContentPane().add( addPanel, BorderLayout.NORTH );
    renderer.setSize( 640, 640 );
    renderer.validate();
    renderer.setVisible( true );
  }

  private static final class AddAction
  implements ActionListener
  {
    private final SetModel setModel;
    private final JTextField label;
    private final JMenu removeMenu;
    
    public AddAction( final SetModel m, final JTextField tf, final JMenu menu )
    {
      setModel = m;
      label = tf;
      removeMenu = menu;
    }
    
    @Override
    public void actionPerformed( final ActionEvent e ) 
    {
      final IRNode node = new PlainIRNode();
      setModel.addNode(
        node, new AVPair[] { new AVPair( LabeledSet.LABEL, label.getText() ) } );
      new RemoveAction( setModel, removeMenu, node, label.getText() );
    }
  }
  
  private static final class RemoveAction
  extends AbstractAction
  {
    private final SetModel setModel;
    private final IRNode node;
    private final JMenu removeMenu;
    private final JMenuItem self;
    
    public RemoveAction(
      final SetModel m, final JMenu menu, final IRNode n, final String l )
    {
      super( "Remove " + l + " [" + n + "]" );
      setModel = m;
      node = n;
      removeMenu = menu;
      self = removeMenu.add( this );
    }
                       
    @Override
    public void actionPerformed( final ActionEvent e )
    {
      setModel.removeNode( node );
      removeMenu.remove( self );
    }
  }
}
