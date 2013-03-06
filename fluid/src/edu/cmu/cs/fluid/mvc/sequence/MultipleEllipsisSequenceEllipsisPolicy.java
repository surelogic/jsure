/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/sequence/MultipleEllipsisSequenceEllipsisPolicy.java,v 1.13 2006/03/29 19:54:51 chance Exp $ */
package edu.cmu.cs.fluid.mvc.sequence;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Policy that inserts one ellipsis for each contiguous sub-sequence
 * of skipped nodes.
 *
 * @author Aaron Greenhouse
 */
public class MultipleEllipsisSequenceEllipsisPolicy
implements SequenceEllipsisPolicy
{
  /** The ConfigurableSequenceView */
  private final ConfigurableSequenceView configView;

  /** Stack of EllipsisRecords */
  private final List<EllipsisRecord> records;

  /** The current set of ellided nodes */
  private Set<IRNode> nodes;

  /** The current location */
  private int currentLoc;



  //==============================================================

  private static class EllipsisRecord
  {
    private final int loc;
    private final Set<IRNode> nodes;

    public EllipsisRecord( final int l, final Set<IRNode> n ) 
    {
      loc = l;
      nodes = n;
    }
  }


  //==============================================================

  public MultipleEllipsisSequenceEllipsisPolicy(
    final ConfigurableSequenceView cv )
  {
    configView = cv;
    nodes = new HashSet<IRNode>();
    records = new LinkedList<EllipsisRecord>();
    currentLoc = -1;
  }


  
  //==============================================================

  @Override
  public void resetPolicy()
  {
    nodes.clear();
    records.clear();
    currentLoc = -1;
  }

  @Override
  public void nodeSkipped( final IRNode node, final int loc )
  {
    if( loc != currentLoc ) {
      records.add( 0, new EllipsisRecord( currentLoc, nodes ) );
      nodes = new HashSet<IRNode>();
      currentLoc = loc;
    }
    nodes.add( node );
  }

  @Override
  public void applyPolicy()
  {
    // Push the last location
    EllipsisRecord r = new EllipsisRecord( currentLoc, nodes );
    
    // unroll while so that we can do a special case for the first ellipsis
    // to be inserted (which is the only one that could be the last node
    // in the sequence
    if( r.loc != -1 ) {
      if( r.loc != 0 && r.loc == configView.size() ) {
        configView.insertEllipsisAfter( configView.location( r.loc-1 ), r.nodes );
      } else  {
        configView.insertEllipsisBefore( configView.location( r.loc ), r.nodes );
      }
      r = records.remove( 0 );
      while( r.loc != -1 ) {    
        configView.insertEllipsisBefore( configView.location( r.loc ), r.nodes );
        r = records.remove( 0 );
      }
    }
  }
}
