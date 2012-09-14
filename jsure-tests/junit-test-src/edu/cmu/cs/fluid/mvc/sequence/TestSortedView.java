/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/TestSortedView.java,v 1.8 2006/03/29 19:54:51 chance Exp $
 *
 * TestSortedView.java
 * Created on March 7, 2002, 10:59 AM
 */

package edu.cmu.cs.fluid.mvc.sequence;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.SimpleAttributeInheritancePolicy;
import edu.cmu.cs.fluid.mvc.SimpleModelRenderer;
import edu.cmu.cs.fluid.mvc.set.LabeledSet;
import edu.cmu.cs.fluid.mvc.set.LabeledSetFactory;
import edu.cmu.cs.fluid.mvc.set.SetModel;
import edu.cmu.cs.fluid.ir.PlainIRNode;
import edu.cmu.cs.fluid.ir.SimpleExplicitSlotFactory;

/**
 * Program to test the SortedViewImpl.
 *
 * @author Aaron Greenhouse
 */
public class TestSortedView
{
  public static void main( final String[] args )
  throws Exception
  {
    final SetModel set =
      LabeledSetFactory.prototype.create(
        "test set", SimpleExplicitSlotFactory.prototype );
    for( int i = 0; i < args.length; i++ ) {
      set.addNode( new PlainIRNode(), 
                   new AVPair[] { new AVPair( LabeledSet.LABEL, args[i] ) } );
    }
    
    final SortedView sortedSet = 
      SortedViewFactory.prototype.create( 
        "Sorted set", set, LabeledSet.LABEL, true,
        SimpleAttributeInheritancePolicy.prototype );

    final JFrame renderer = new SimpleModelRenderer( sortedSet );
    final JMenu dirMenu = new JMenu( "Sort Dir" );
    dirMenu.add( new SortDirAction( sortedSet, true ) );
    dirMenu.add( new SortDirAction( sortedSet, false ) );
    dirMenu.addSeparator();
    dirMenu.add( new SortDirConvienenceAction( sortedSet, true ) );
    dirMenu.add( new SortDirConvienenceAction( sortedSet, false ) );
    
    final JMenu pivotMenu = new JMenu( "Sort by" );
    final Iterator<String> iter = set.getNodeAttributes();
    while( iter.hasNext() ) {
      final String attr = iter.next();
      pivotMenu.add( new SortAction( sortedSet, attr ) );
      pivotMenu.add( new SortConvienenceAction( sortedSet, attr ) );
    }
    
    final JMenuBar menuBar = new JMenuBar();
    menuBar.add( dirMenu );
    menuBar.add( pivotMenu );
    renderer.setJMenuBar( menuBar );
    renderer.setSize( 640, 640 );
    renderer.validate();
    renderer.setVisible( true );
  }

  
  
  private static final class SortDirAction
  extends AbstractAction
  {
    private SortedView model;
    private Boolean value;
    
    public SortDirAction( final SortedView sv, final boolean v )
    {
      super( "Set isAscending to " + v );
      model = sv;
      value = v ? Boolean.TRUE : Boolean.FALSE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void actionPerformed( final ActionEvent e )
    {
      model.getCompAttribute( SortedView.IS_ASCENDING ).setValue( value );
    }
  }

  
  
  private static final class SortDirConvienenceAction
  extends AbstractAction
  {
    private SortedView model;
    private boolean value;
    
    public SortDirConvienenceAction( final SortedView sv, final boolean v )
    {
      super( "setAscending(" + v + ")" );
      model = sv;
      value = v;
    }
    
    @Override
    public void actionPerformed( final ActionEvent e )
    {
      model.setAscending( value );
    }
  }

  
  
  private static final class SortAction
  extends AbstractAction
  {
    private SortedView model;
    private String attr;
    
    public SortAction( final SortedView sv, final String a )
    {
      super( "Sort by \"" + a + "\"" );
      model = sv;
      attr = a;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void actionPerformed( final ActionEvent e )
    {
      model.getCompAttribute( SortedView.SORT_ATTR ).setValue( attr );
    }
  }

  
  
  private static final class SortConvienenceAction
  extends AbstractAction
  {
    private SortedView model;
    private String attr;
    
    public SortConvienenceAction( final SortedView sv, final String a )
    {
      super( "setSortAttribute(\"" + a + "\")" );
      model = sv;
      attr = a;
    }
    
    @Override
    public void actionPerformed( final ActionEvent e )
    {
      model.setSortAttribute( attr );
    }
  }
}
