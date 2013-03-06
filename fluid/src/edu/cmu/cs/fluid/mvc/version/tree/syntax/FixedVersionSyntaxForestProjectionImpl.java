package edu.cmu.cs.fluid.mvc.version.tree.syntax;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.tree.syntax.SyntaxForestModel;
import edu.cmu.cs.fluid.mvc.version.VersionTrackerModel;
import edu.cmu.cs.fluid.mvc.version.tree.AbstractFixedVersionForestProjection;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.version.Version;

/**
 * Minimal implementation of FixedVersionSyntaxForestProjection.
 */
final class FixedVersionSyntaxForestProjectionImpl
extends AbstractFixedVersionForestProjection
implements FixedVersionSyntaxForestProjection
{
  private final SyntaxForestModel srcAsSyntax;
  
  
  
  //===========================================================
  //== Constructor
  //===========================================================
  
  protected FixedVersionSyntaxForestProjectionImpl(
    final String name, final SyntaxForestModel srcModel,
    final VersionTrackerModel vc, final ModelCore.Factory mf,
    final ViewCore.Factory vf, final Version initVersion )
  throws SlotAlreadyRegisteredException
  {
    super( name, srcModel, vc, mf, vf, initVersion );
    srcAsSyntax = srcModel;
    finalizeInitialization();
  }

  
  
  //===========================================================
  //== SyntaxForest methods
  //===========================================================
  
  @Override
  public Operator getOperator( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsSyntax.getOperator( node );
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public boolean opExists( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsSyntax.opExists( node );
    } finally {
      Version.restoreVersion();
    }
  }

  @Override
  public void initNode( final IRNode n, final Operator op )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  @Override
  public void initNode( final IRNode n, final Operator op, final int min )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  @Override
  public void initNode(
    final IRNode n, final Operator op, final IRNode[] children )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
}
