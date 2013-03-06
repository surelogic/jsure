// $Header:
// /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/ConfigurableSyntaxForestViewImpl.java,v
// 1.7 2003/07/15 18:39:13 thallora Exp $

package edu.cmu.cs.fluid.mvc.tree.syntax;

import java.util.Set;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.tree.*;
import edu.cmu.cs.fluid.mvc.visibility.VisibilityModel;
import edu.cmu.cs.fluid.parse.Ellipsis;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A minimal implementation of {@link ConfigurableSyntaxForestView}/
 */
final class ConfigurableSyntaxForestViewImpl
  extends AbstractConfigurableForest
  implements ConfigurableSyntaxForestView {
  private final SyntaxForestModelCore synModCore;

  private final SyntaxForestModel srcAsSyntax;

  //===========================================================
  //== Constructor
  //===========================================================

  protected ConfigurableSyntaxForestViewImpl(
    final String name,
    final SyntaxForestModel src,
    final VisibilityModel vizModel,
    final ModelCore.Factory mf,
    final ViewCore.Factory vf,
    final ForestModelCore.Factory fmf,
    final SyntaxForestModelCore.Factory smf,
    final ConfigurableViewCore.Factory cvf,
    final AttributeInheritancePolicy aip,
    final ForestProxyAttributePolicy pp,
    final ForestEllipsisPolicy ePolicy,
    final boolean ef,
    final boolean ep)
    throws SlotAlreadyRegisteredException {
    // Init model parts
    super(
      name,
      src,
      vizModel,
      mf,
      vf,
      fmf,
      cvf,
      aip,
      pp,
      ePolicy,
      ef,
      ep);
    srcAsSyntax = src;
    synModCore = smf.create(name, this, structLock, attrManager);

    rebuildModel();
    finalizeInitialization();
  }

  //===========================================================
  //== Ellipsis Insertion (From ConfigurabeSyntaxForestView)
  //===========================================================

  @Override
  protected ForestVerticalEllipsisPolicy getVerticalEllipsisPolicy() {
    final ForestVerticalEllipsisPolicy ep = new MultipleEllipsisSyntaxVerticalEllipsisPolicy(this, true);
    return ep;
  }
  
  // Model will/must already be locked when this is called
  @Override
  public void setEllipsisAt(
      final IRNode ellipsis, 
    final IRNode parent,
    final IRLocation loc,
    final Set<IRNode> nodes) {
//    final IRNode ellipsis = new PlainIRNode();
//    setupEllipsisNode(ellipsis);
    if (parent == null) {
      final InsertionPoint ip = InsertionPoint.createBefore(loc);
      forestModCore.insertRootAt(ellipsis, ip);
    } else {
      forestModCore.setSubtree(parent, loc, ellipsis); // FIX not always right
    }
    finalizeEllipsis(parent, ellipsis, nodes);
  }

  //===========================================================
  //== Exported model builder methods
  //===========================================================

  @Override
  protected void setupNode(final IRNode n) {
    try {
      synModCore.initNode(n, srcAsSyntax.getOperator(n));
    } catch (final NullPointerException e) {
      MV.fine("Null exception (only during init): " + n);
      MV.fine("model core = " + synModCore);
      MV.fine("source model = " + srcAsSyntax);
      MV.fine("op = " + srcAsSyntax.getOperator(n));
      // srcAsSyntax = (SyntaxForestModel) srcModel;
      // synModCore = (SyntaxForestModelCore) forestModCore;
      //synModCore.initNode( n, srcAsSyntax.getOperator(n) );
    }
  }

  @Override
  protected void setupEllipsisNode(final IRNode n) {
    try {
      synModCore.initNode(n, Ellipsis.prototype);
    } catch (NullPointerException e) {
      /*
			 * What the heck is this stuff? I don't think it is needed anymore?
			 * synForestModCore = (SyntaxForestModelCore) forestModCore;
			 * synForestModCore.initNode( n, Ellipsis.prototype );
			 */
    }
  }

  @Override
  protected void addSubtree(final IRNode parent, final IRNode n,
      final boolean sameParent, final int oldPos) {

    // Set the node in the specific position if the parent is the same as the
    // parent in the source model and the parent's operator has a fixed
    // number of children.
    if (sameParent && synModCore.getOperator(parent).numChildren() >= 0) {
//      System.out.println("parent's op == " + synModCore.getOperator(parent).name());
      forestModCore.setSubtree(parent, oldPos, n);
    } else {
      forestModCore.appendSubtree(parent, n);
    }
//    // final Operator op = srcAsSyntax.getOperator(parent);
//    final Operator op = synModCore.getOperator(parent);
//    final int i = op.numChildren();
//
//    if (i < 0) {
//      forestModCore.appendSubtree(parent, n);
//      // FIX may have to go in a particular "first" slot
//    } else {
//      // Assume that n will be inserted as a subtree of one of its ancestors
//      //
//      // Search up from n in srcS to find parent
//      final int pos =
//        (synModCore.getOperator(n) instanceof Ellipsis)
//          ? positionEllipsis(parent, n, i)
//          : positionNode(parent, n);
//      forestModCore.setSubtree(parent, pos, n);
//    }
  }

//  protected int positionNode(final IRNode parent, final IRNode n) {
//    IRNode here = n;
//    while (here != null) {
//      final IRNode anc = srcAsSyntax.getParent(here);
//      if (anc == parent) {
//        break;
//      } else {
//        here = anc;
//      }
//    }
//    if (here == null) {
//      throw new FluidError(
//        "Couldn't find " + parent + " as an ancestor of " + n);
//    }
//    return srcAsSyntax.childLocationIndex(
//      parent,
//      srcAsSyntax.getLocation(here));
//  }

//  protected int positionEllipsis(
//    final IRNode parent,
//    final IRNode n,
//    final int max) {
//    // Assume n has no parents being an ellipsis
//    for (int i = 0; i < max; i++) {
//      if (forestModCore.getChild(parent, i) == null) {
//        return i;
//      }
//    }
//    return 0;
//  }

  //===========================================================
  //== SyntaxForestModel methods
  //===========================================================

  @Override
  public Operator getOperator(final IRNode n) {
    synchronized (structLock) {
      return synModCore.getOperator(n);
    }
  }

  @Override
  public boolean opExists(final IRNode n) {
    synchronized (structLock) {
      return synModCore.opExists(n);
    }
  }

  @Override
  public void initNode(final IRNode n, final Operator op) {
    synchronized (structLock) {
      synModCore.initNode(n, op);
    }
  }

  @Override
  public void initNode(final IRNode n, final Operator op, final int min) {
    synchronized (structLock) {
      synModCore.initNode(n, op, min);
    }
  }

  @Override
  public void initNode(
    final IRNode n,
    final Operator op,
    final IRNode[] children) {
    synchronized (structLock) {
      synModCore.initNode(n, op, children);
    }
  }
}
