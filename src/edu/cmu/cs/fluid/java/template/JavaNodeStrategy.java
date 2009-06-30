/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/template/JavaNodeStrategy.java,v 1.8 2007/07/10 22:16:36 aarong Exp $ */
package edu.cmu.cs.fluid.java.template;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.template.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Scalar strategy that accepts JavaNodes based on their operator type.
 *
 * @author Aaron Greenhouse
 */
@Deprecated
@SuppressWarnings("all")
public class JavaNodeStrategy
extends IRNodeStrategy
{
  /**
   * Create a new strategy that accepts JavaNodes with the given operators.
   * @param ops Array of acceptable operators.
   */
  public JavaNodeStrategy( final Operator[] ops )
  {
    super( JJNode.tree, ops );
  }

  /**
   * Create a new strategy that accepts any IRNode.
   */
  public JavaNodeStrategy()
  {
    this( ANY_IRNODE );
  }

  // Inherit JavaDoc
  @Override
  protected boolean correctNodeType( final IRNode node )
  {
    return JJNode.tree.isNode( node );
  }

  protected static class JavaNodeVectorDescriptor
  extends IRNodeVectorDescriptor
  {
    public JavaNodeVectorDescriptor( final int min, final int max,
                          Operator[][] fN, final Operator[] r )
    {
      super( min, max, fN, r, JJNode.tree );
    }

    @Override
    public FieldStrategy getStrategyForPos( final int n )
    {
      return new JavaNodeStrategy( fN[n] );
    }

    @Override
    public FieldStrategy getStrategyForRest()
    {
      return new JavaNodeStrategy( rest );
    }

  }
}
