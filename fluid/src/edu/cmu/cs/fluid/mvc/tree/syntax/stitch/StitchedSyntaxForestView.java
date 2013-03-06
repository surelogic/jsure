// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/tree/syntax/stitch/StitchedSyntaxForestView.java,v 1.7 2007/03/09 16:45:24 chance Exp $

package edu.cmu.cs.fluid.mvc.tree.syntax.stitch;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.*;
import edu.cmu.cs.fluid.mvc.tree.*;
import edu.cmu.cs.fluid.mvc.tree.stitch.*;
import edu.cmu.cs.fluid.mvc.tree.stitch.IStitchTreeTransform.AttributeHandler;
import edu.cmu.cs.fluid.mvc.tree.syntax.*;
import edu.cmu.cs.fluid.tree.Operator;

/**
 */
final class StitchedSyntaxForestView
  extends AbstractStitchedForest 
  implements SyntaxForestModel
{
  private final SyntaxForestModelCore synModCore;

  //private final SyntaxForestModel srcAsSyntax;

  //===========================================================
  //== Constructor
  //===========================================================

  @SuppressWarnings("deprecation")
  protected StitchedSyntaxForestView(
    final String name,
    final SyntaxForestModel src,
    final ModelCore.Factory mf,
    final ViewCore.Factory vf,
    final ForestModelCore.Factory fmf,
    final SyntaxForestModelCore.Factory smf,
    final AttributeInheritancePolicy aip,
    final IStitchTreeTransform.Factory xf)
    throws SlotAlreadyRegisteredException 
  {
    // Init model parts
    super(
      name,
      src,
      mf,
      vf,
      fmf,
      aip);
    
    //srcAsSyntax = src;
    synModCore = smf.create(name, this, structLock, attrManager);

    if (xf == null) {
      transform = new LFVTreeTransform(forestModCore, synModCore);
    } else {
      transform = xf.create(this, forestModCore, synModCore);
    }
    transform.init(new AttributeHandler() {
      @Override
      @SuppressWarnings("unchecked")
      public SlotInfo addNodeAttribute(String name, IRType type, SlotFactory sf, boolean mutable) {
        SlotInfo si = null;
        try {
          si = sf.newAttribute(StitchedSyntaxForestView.this.getName()+name, type);
          attrManager.addNodeAttribute(name, Model.USER_DEFINED, si, userDefinedCallback);
        } catch (SlotAlreadyRegisteredException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return si;
      }
    });
    
    rebuildModel();
    finalizeInitialization();
    

  }

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
