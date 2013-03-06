/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/SingleEllipsisSequenceEllipsisPolicy.java,v 1.11 2005/07/01 16:15:36 chance Exp $ */
package edu.cmu.cs.fluid.mvc.sequence;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.fluid.ir.IRNode;


/**
 * Policy that inserts at most one ellipsis into the sequence.
 * The ellipsis may be either at the top or the bottom of the
 * sequence.
 *
 * @author Aaron Greenhouse
 */
public class SingleEllipsisSequenceEllipsisPolicy
implements SequenceEllipsisPolicy
{
  public static final boolean AT_TOP = true;
  public static final boolean AT_BOTTOM = false;
  
  /** The ConfigurableSequenceView */
  private final ConfigurableSequenceView configView;

  /** Whether to put the ellipsis at the top or bottom. */
  private final boolean atTop;

  /** The ellided nodes */
  private final Set<IRNode> nodes;



  //==============================================================

  public SingleEllipsisSequenceEllipsisPolicy(
    final ConfigurableSequenceView cv, final boolean loc )
  {
    configView = cv;
    atTop = loc;
    nodes = new HashSet<IRNode>();
  }

  public boolean isAtBottom()
  {
    return !atTop;
  }

  //==============================================================

  @Override
  public void resetPolicy()
  {
    nodes.clear();
  }

  @Override
  public void nodeSkipped( final IRNode node, final int loc )
  {
    nodes.add( node );
  }

  @Override
  public void applyPolicy()
  {
    if( nodes.size() > 0 ) {
      if( atTop ) {
        configView.insertEllipsisBefore( configView.firstLocation(), nodes );
      } else {
        configView.insertEllipsisAfter( configView.lastLocation(), nodes );
      }
    }
  }
}
