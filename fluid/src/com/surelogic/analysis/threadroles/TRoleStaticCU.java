/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticCU.java,v 1.8 2007/07/09 13:52:31 chance Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.*;

import com.surelogic.dropsea.ir.drops.promises.threadroles.TRoleIncompatibleDrop;
import com.surelogic.dropsea.ir.drops.promises.threadroles.TRoleRenameDrop;

import edu.cmu.cs.fluid.ir.IRNode;


public class TRoleStaticCU extends TRoleStaticWithChildren {

  private static final Map<IRNode, TRoleStaticCU> nodeToCU = new HashMap<IRNode, TRoleStaticCU>();
  
  private static final StaticStructInvalidator invalidator = new StaticStructInvalidator();

  Set<TRoleRenameDrop> trRenames;
  Collection<TRoleIncompatibleDrop> trIncompatibles;
  
//Set<ColorStaticMeth> methInThisCU;
//Map<IRNode, ColorStaticMeth> methMap;

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure#accept(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.IColorVisitor)
   */
  @Override
  public void accept(ITRoleStaticVisitor visitor) {
    visitor.visitCU(this);
  }

  public TRoleStaticCU(final IRNode node, final TRoleStaticWithChildren parent) {
    super(node, parent);
    // create the phantom drop that will be responsible for calling the
    // invalidateAction method when it's time to clean up this CU's structure.
    final TRolePhantomStructure invalidator = new TRolePhantomStructure(node);
    // register the mapping from IRNode to this static structure thingy.
    synchronized(TRoleStaticCU.class) {
      nodeToCU.put(node, this);
    }

  }


  /**
   * Called when the IR for a CU is about to be nuked. This is our chance to
   * clean up both static and dynamic structure, and give the GC'tor some
   * garbage to work with.
   * @param node The IRNode for a CU that is no longer valid.
   */
  protected static synchronized void invalidateAction(final IRNode node) {

    final TRoleStaticCU theStatCU = nodeToCU.remove(node);

    if (theStatCU != null) {
      invalidator.visit(theStatCU);
    }

  }

  private static class StaticStructInvalidator extends TRoleStructVisitor {

    @Override
    public void visit(TRoleStaticStructure node) {
      if (node instanceof TRoleStaticWithChildren) {
	doAcceptForChildren((TRoleStaticWithChildren) node);
      }
    }
    
    

//    @Override
//    public void visitClass(ColorStaticClass node) {
//      node.invalidateAction();
//      super.visitClass(node);
//    }



    @Override
    public void visitMeth(TRoleStaticMeth node) {
      node.invalidateAction();
      super.visitMeth(node);
    }

  }

  public static TRoleStaticCU getStaticCU(final IRNode node) {
    TRoleStaticCU res;
    synchronized (TRoleStaticCU.class) {
      res = nodeToCU.get(node);
      if (res != null) {
	return res;
      }
    }
    // note that the locking isn't quite right here!
    TRolesFirstPass.getInstance().buildStaticStructureForACU(node);
    synchronized (TRoleStaticCU.class) {
      res = nodeToCU.get(node);
    }
    return res;
  }

//public synchronized void addMeth(ColorStaticMeth meth) {
//if (methMap == null) {
//methMap = new HashMap<IRNode, ColorStaticMeth>();
//}
//methMap.put(meth.getNode(), meth);
//}

//public synchronized ColorStaticMeth findMeth(final IRNode node) {
//if (methMap == null) {
//return null;
//}

//final ColorStaticMeth meth = methMap.get(node);
//return meth;
//}
}
