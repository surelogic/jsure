package edu.cmu.cs.fluid.mvc.version.tree;

import java.util.*;

import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.ViewCore;
import edu.cmu.cs.fluid.mvc.tree.ForestModel;
import edu.cmu.cs.fluid.mvc.version.AbstractFixedVersionProjection;
import edu.cmu.cs.fluid.mvc.version.VersionTrackerModel;
import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.InsertionPoint;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.tree.DigraphListener;
import edu.cmu.cs.fluid.tree.StructureException;
import edu.cmu.cs.fluid.version.Version;

/**
 * Abstract implementation of a Fixed Version Forest Projection. 
 */
@SuppressWarnings("deprecation")
public abstract class AbstractFixedVersionForestProjection
extends AbstractFixedVersionProjection
{
  protected final ForestModel srcAsForest;



  //===========================================================
  //== Constructor
  //===========================================================
  
  protected AbstractFixedVersionForestProjection(
    final String name, final ForestModel srcModel, final VersionTrackerModel vc,
    final ModelCore.Factory mf, final ViewCore.Factory vf,
    final Version initVersion )
  throws SlotAlreadyRegisteredException
  {
    super( name, srcModel, vc, mf, vf, initVersion );
    srcAsForest = srcModel;
  }


  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin DigraphModelPortion
  //-- (Move this to superclass later)
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Digraph Methods 
  //===========================================================

  public final SlotInfo getAttribute( final String name ) {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.getAttribute( name );
    } finally {
      Version.restoreVersion();
    }
  }
  
  public final boolean hasChildren( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.hasChildren( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final int numChildren( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.numChildren( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRLocation childLocation( final IRNode node, final int i )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.childLocation( node, i );
    } finally {
      Version.restoreVersion();
    }
  }

  public final int childLocationIndex( final IRNode node, final IRLocation loc )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.childLocationIndex( node, loc );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRLocation firstChildLocation( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.firstChildLocation( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRLocation lastChildLocation( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.lastChildLocation( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRLocation nextChildLocation( final IRNode node, final IRLocation loc )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.nextChildLocation( node, loc );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRLocation prevChildLocation( final IRNode node, final IRLocation loc )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.prevChildLocation( node, loc );
    } finally {
      Version.restoreVersion();
    }
  }

  public final int compareChildLocations( final IRNode node, final IRLocation loc1,
                                    final IRLocation loc2 )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.compareChildLocations( node, loc1, loc2 );
    } finally {
      Version.restoreVersion();
    }
  }

  public final boolean hasChild( final IRNode node, final int i )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.hasChild( node, i );
    } finally {
      Version.restoreVersion();
    }
  }

  public final boolean hasChild( final IRNode node, final IRLocation loc )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.hasChild( node, loc );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRNode getChild( final IRNode node, final int i )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.getChild( node, i );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRNode getChild( final IRNode node, final IRLocation loc ) 
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.getChild( node, loc );
    } finally {
      Version.restoreVersion();
    }
  }

  public final Iteratable<IRNode> children( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.children( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final List<IRNode> childList( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.childList( node );
    } finally {
      Version.restoreVersion();
    }
  }
  
  public final void addDigraphListener( final DigraphListener dl )
  {
    srcAsForest.addDigraphListener( dl );
  }

  public final void removeDigraphListener( DigraphListener dl )
  {
    srcAsForest.removeDigraphListener( dl );
  }

  public void addObserver(Observer o) {
    throw new UnsupportedOperationException( "Observers are obsolete here." );
  }

  //===========================================================
  //== Mutable Digraph Methods
  //===========================================================

  // This does not break the model!
  public final void initNode( final IRNode n )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  // This does not break the model!
  public final void initNode( final IRNode n, final int numChildren )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final boolean isNode( final IRNode n )
  {
    return srcAsForest.isNode( n );
  }

  public final void setChild( final IRNode node, final int i, 
			final IRNode newChild ) 
    throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void setChild( final IRNode node, final IRLocation loc,
                        final IRNode newChild ) 
    throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void addChild( final IRNode node, final IRNode newChild )
    throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void replaceChild( final IRNode node, final IRNode oldChild,
                            final IRNode newChild )
    throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final IRLocation insertChild( final IRNode node, final IRNode newChild,
                                 final InsertionPoint ip )
    throws StructureException    
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void insertChild( final IRNode node, final IRNode newChild )
    throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void appendChild( final IRNode node, final IRNode newChild )
    throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void insertChildAfter( final IRNode node, final IRNode newChild,
                                final IRNode oldChild )
    throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void insertChildBefore( final IRNode node, final IRNode newChild,
                                 final IRNode oldChild )
    throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void removeChild( final IRNode node, final IRNode oldChild )
    throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void removeChild( final IRNode node, final IRLocation loc )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void removeChildren( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final Iteratable<IRNode> depthFirstSearch( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.depthFirstSearch( node );
    } finally {
      Version.restoreVersion();
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End DigraphModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------



  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin Symmetric Digraph Model Portion
  //-- (Move this to super class later)
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Symmetric Digraph Methods 
  //===========================================================

  public final boolean hasParents( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.hasParents( node );
    } finally {
      Version.restoreVersion();
    }
  }
  
  public final int numParents( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.numParents( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRLocation parentLocation( final IRNode node, final int i )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.parentLocation( node, i );
    } finally {
      Version.restoreVersion();
    }
  }

  public final int parentLocationIndex( final IRNode node, final IRLocation loc )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.parentLocationIndex( node, loc );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRLocation firstParentLocation( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.firstParentLocation( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRLocation lastParentLocation( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.lastParentLocation( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRLocation nextParentLocation( final IRNode node, 
					final IRLocation ploc )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.nextParentLocation( node, ploc );
    } finally {
      Version.restoreVersion();
    }
  }
  
  public final IRLocation prevParentLocation( final IRNode node, 
					final IRLocation ploc )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.prevParentLocation( node, ploc );
    } finally {
      Version.restoreVersion();
    }
  }

  public final int compareParentLocations( final IRNode node, final IRLocation loc1,
                                     final IRLocation loc2 )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.compareParentLocations( node, loc1, loc2 );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRNode getParent( final IRNode node, final int i )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.getParent( node, i );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRNode getParent( final IRNode node, final IRLocation loc )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.getParent( node, loc );
    } finally {
      Version.restoreVersion();
    }
  }

  public final Iteratable<IRNode> parents( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.parents( node );
    } finally {
      Version.restoreVersion();
    }
  }



  //===========================================================
  //== Mutable Symmetric Digraph Methods
  //===========================================================

  // This does not break the model!
  public final void initNode( final IRNode n, final int numParents,
                        final int numChildren )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void setParent( final IRNode node, final int i, 
			 final IRNode newParent )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void setParent( final IRNode node, final IRLocation loc,
                         final IRNode newParent )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void addParent( final IRNode node, final IRNode newParent )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void removeParent( final IRNode node, final IRNode parent )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void replaceParent( final IRNode node, final IRNode oldParent,
                             final IRNode newParent )
       throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void removeParents( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  
  public final Iteratable<IRNode> connectedNodes( final IRNode root )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.connectedNodes( root );
    } finally {
      Version.restoreVersion();
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End Symmetric Digraph
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  
  
  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- Begin ForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------

  //===========================================================
  //== Tree Methods
  //===========================================================

  public final IRNode getParent( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.getParent( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRLocation getLocation( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.getLocation( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final IRNode getRoot( final IRNode subtree )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.getRoot( subtree );
    } finally {
      Version.restoreVersion();
    }
  }

  public final Iteratable<IRNode> bottomUp( final IRNode subtree )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.bottomUp( subtree );
    } finally {
      Version.restoreVersion();
    }
  }

  public final Iteratable<IRNode> topDown( final IRNode subtree )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.topDown( subtree );
    } finally {
      Version.restoreVersion();
    }
  }



  //===========================================================
  //== Mutable Tree Methods
  //===========================================================

  public final void clearParent( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final IRNode getParentOrNull( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.getParentOrNull( node );
    } finally {
      Version.restoreVersion();
    }
    // throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void setSubtree( final IRNode parent, final int i,
                          final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void setSubtree( final IRNode parent, final IRLocation loc,
                          final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void replaceSubtree( final IRNode oldChild, final IRNode newChild )
      throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void exchangeSubtree( final IRNode node1, final IRNode node2 )
      throws StructureException
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final IRLocation insertSubtree( final IRNode node, final IRNode newChild,
                                   final InsertionPoint ip )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void insertSubtree( final IRNode parent, final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void appendSubtree( final IRNode parent, final IRNode newChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void removeSubtree( final IRNode node )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void insertSubtreeAfter( final IRNode newChild, 
				  final IRNode oldChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void insertSubtreeBefore( final IRNode newChild, 
				   final IRNode oldChild )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final Iteratable<IRNode> rootWalk( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.rootWalk( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final int comparePreorder( final IRNode node1, final IRNode node2 )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.comparePreorder( node1, node2 );
    } finally {
      Version.restoreVersion();
    }
  }



  //===========================================================
  //== Methods for dealing with roots
  //===========================================================

  public final boolean isRoot( final IRNode node )
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.isRoot( node );
    } finally {
      Version.restoreVersion();
    }
  }

  public final void removeRoot( final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  public final void addRoot( final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  public final void insertRoot( final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void insertRootBefore( final IRNode newRoot, final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void insertRootAfter( final IRNode newRoot, final IRNode root )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final void insertRootAt( final IRNode root, final IRLocation loc )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }
  
  public final void insertRootAt( final IRNode root, final InsertionPoint pt )
  {
    throw new UnsupportedOperationException( "Cannot modify a projection." );
  }

  public final Iteratable<IRNode> getRoots()
  {
    Version.saveVersion();
    try {
      Version.setVersion( tracker.getVersion() );
      return srcAsForest.getRoots();
    } finally {
      Version.restoreVersion();
    }
  }

  //-----------------------------------------------------------
  //-----------------------------------------------------------
  //-- End ForestModel Portion
  //-----------------------------------------------------------
  //-----------------------------------------------------------
}
