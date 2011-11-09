/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/SimpleModelRenderer.java,v 1.5 2003/07/15 18:39:10 thallora Exp $
 *
 * SimpleModelRenderer.java
 * Created on March 22, 2002, 11:06 AM
 */

package edu.cmu.cs.fluid.mvc;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;

/**
 * Render a model in a frame using a bare bones representation.
 *
 * @author Aaron Greenhouse
 */
public final class SimpleModelRenderer
extends JFrame
{
  /** Creates a new instance of SimpleModelRenderer */
  public SimpleModelRenderer( final Model model )
  {
    super( "Model \"" + model.getName() + "\"" );
    setDefaultCloseOperation( EXIT_ON_CLOSE );
    
    final Container contentPane = getContentPane();
    contentPane.setLayout( new BorderLayout() );
    contentPane.add( new SimpleRenderer( model ), BorderLayout.CENTER );
    setSize( 640, 480 );
    validate();
  }
}
