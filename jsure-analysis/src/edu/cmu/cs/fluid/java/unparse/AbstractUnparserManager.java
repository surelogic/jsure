package edu.cmu.cs.fluid.java.unparse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.display.*;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.java.JavaFmtStream;
import edu.cmu.cs.fluid.tree.SyntaxTreeInterface;
import edu.cmu.cs.fluid.unparse.*;

/**
 * Class for managing all the unparser related stuff.  Objects of this
 * class are thread-safe: Any combination of methods may be called
 * concurrently.
 */
public class AbstractUnparserManager implements IUnparserManager
{
  protected static final Logger LOG =
	  SLLogger.getLogger("ECLIPSE.unparserEditor");
    
  /** The root node of the parse tree to be unparsed. */
  protected final List<IRNode> parseRoots = new ArrayList<IRNode>();
  
  /** The unparser to use. */
  protected final JavaFmtStream unparser;
  
  /** The tokens to unparse. */
  protected TokenArray tokens;
  
  /**
   * The current tokenized view generated from the token array.
   */
  protected TokenView tview = null;

  /**
   * The unparsed source code.  Need to keep this around to get information
   * about the token under the mouse pointer.
   */
  protected String[] unparsed = null;
  
  
  /**
   * Create a new unparser manager for the given root node.
   * @param root The root node of the parse tree to be unparsed.
   * @param initWidth The initial width to use for unparsing.
   */
  // FIX allow for multiple roots
  public AbstractUnparserManager( final IRNode root, 
                                   final SyntaxTreeInterface tree,
                                   final int initWidth )
  {
    parseRoots.add(root);
    unparser = new JavaFmtStream( false, tree );
    unparser.getStyle().setUnparsePromises( true );
    updateWithWidth( initWidth );
  }
  
  public AbstractUnparserManager( final Collection<IRNode> roots,
                                   final SyntaxTreeInterface tree,
                                   final int initWidth )
  {
    parseRoots.addAll(roots);
    unparser = new JavaFmtStream( false, tree );
    unparser.getStyle().setUnparsePromises( true );
    updateWithWidth( initWidth );    
  } 
  @Override
  public Iterator<IRNode> getRoots() { return parseRoots.iterator(); }
  
  /**
   * Get the unparsed tree.
   * @return An array of Strings of the unparsed text.  <em>Warning</em>:
   * The array is shared, and should not be modified by the caller.
   */
  @Override
  public synchronized String[] getUnparsedText()
  {
    return unparsed;
  }
  
  /**
   * Redo the unparsing using the specified line width.  This should be used
   * when the parse tree is unchanged, but the region in which it is displayed
   * has changed width.  It should not be used, for example, when the current
   * version has changed.
   */
  @Override
  public synchronized String[] unparseWithWidth( final int lineWidth )
  {
    tview = new TokenView( lineWidth, tokens, false );
    tview.init();
    
    unparsed = tview.strTV();
    return unparsed;
  }
  
  /**
   * Update the tokens and then redo the unparsing using the specified line
   * width.  This should be used when the parse tree itself has changed, for
   * example, because the version has changed.
   */
  @Override
  public synchronized String[] updateWithWidth( final int lineWidth )
  {
    final boolean debug = LOG.isLoggable(Level.FINE);
    unparser.resetStream();
    for(int i=0; i<parseRoots.size(); i++) {
      try {
        if (debug) {
          LOG.fine("Unparsing root #"+i);
        }
        unparser.unparse( parseRoots.get(i) );
        unparser.prepStream();
      } catch(SlotUndefinedException e) {
        LOG.log(Level.SEVERE, "Died doing ", e);
      }
    }
    tokens = unparser.getTokenArray();
    tokens.finish();
    
    return unparseWithWidth( lineWidth );
  }
  
  /**
   * Get the IRNode associated with the text at the given character location.
   * @param where The location of interest.
   * @return The IRNode associated with the text location, or <code>null</code>
   * if there is not node associated with the position.
   */
  @Override
  public synchronized IRNode getNodeAt( final TextCoord where )
  {
    /* For some reason the unparser starts at line 1, and refuses to
     * know anything about line 0.
     */
    return (where.getLine() == 0) ? null : tview.nodeAt( where );
  }
  
  /**
   * Get the TextRegion (the extent of the text) belonging to the IRNode
   * that is associated with the text at the given character location.
   * Returns <code>null</code> if the location doesn't have a node
   * associated with it.
   */
  @Override
  public synchronized TextRegion getNodeRegion( final TextCoord where )
  {
    /* For some reason the unparser starts at line 1, and refuses to
     * know anything about line 0.
     */
    if( where.getLine() == 0 ) {
      return null;
    } else {
      final IRNode node = tview.nodeAt( where );
      if( node != null ) {
        return tview.getNodeRegion( node, unparsed );
      } else {
        return null;
      }
    }
  }
  
  /**
   * Get the TextRegion (the extend of the text) belonging to the given
   * IRNode within the parse tree.
   * @exception NullPointerException Thrown if the given node is null.
   */
  @Override
  public synchronized TextRegion getNodeRegion( final IRNode node )
  {
    try {
      return tview.getNodeRegion( node, unparsed );
    } catch(SlotUndefinedException e) {
      return null;
    }
  }
}
