package edu.cmu.cs.fluid.mvc.sequence;

import edu.cmu.cs.fluid.ir.IRNode;


/**
 * Policy that never inserts any ellipses.
 *
 * @author Aaron Greenhouse
 */
public class NoEllipsisSequenceEllipsisPolicy
implements SequenceEllipsisPolicy
{
  public static final SequenceEllipsisPolicy prototype =
    new NoEllipsisSequenceEllipsisPolicy();

  //==============================================================

  private NoEllipsisSequenceEllipsisPolicy()
  {
  }



  //==============================================================

  @Override
  public void resetPolicy()
  {
  }

  @Override
  public void nodeSkipped( final IRNode node, final int loc )
  {
  }

  @Override
  public void applyPolicy()
  {
  }
}


