/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/ColorStaticCU.java,v 1.9 2007/10/31 21:13:55 dfsuther Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import java.util.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.promises.ColorIncompatibleDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ColorRenameDrop;

@Deprecated
public class ColorStaticCU extends ColorStaticWithChildren {

  private static final Map<IRNode, ColorStaticCU> nodeToCU = new HashMap<IRNode, ColorStaticCU>();
  
  private static final StaticStructInvalidator invalidator = new StaticStructInvalidator();

  Set<ColorRenameDrop> colorRenames;
  Collection<ColorIncompatibleDrop> colorIncompatibles;
  
//Set<ColorStaticMeth> methInThisCU;
//Map<IRNode, ColorStaticMeth> methMap;

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure#accept(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.IColorVisitor)
   */
  @Override
  public void accept(IColorVisitor visitor) {
    visitor.visitCU(this);
  }

  public ColorStaticCU(final IRNode node, final ColorStaticWithChildren parent) {
    super(node, parent);
    // create the phantom drop that will be responsible for calling the
    // invalidateAction method when it's time to clean up this CU's structure.
    final ColorPhantomStructure invalidator = new ColorPhantomStructure(node);
    // register the mapping from IRNode to this static structure thingy.
    synchronized(ColorStaticCU.class) {
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

    final ColorStaticCU theStatCU = nodeToCU.remove(node);

    if (theStatCU != null) {
      invalidator.visit(theStatCU);
    }

  }
  @Deprecated
  private static class StaticStructInvalidator extends ColorStructVisitor {

    @Override
    public void visit(ColorStaticStructure node) {
      if (node instanceof ColorStaticWithChildren) {
	doAcceptForChildren((ColorStaticWithChildren) node);
      }
    }
    
    

//    @Override
//    public void visitClass(ColorStaticClass node) {
//      node.invalidateAction();
//      super.visitClass(node);
//    }



    @Override
    public void visitMeth(ColorStaticMeth node) {
      node.invalidateAction();
      super.visitMeth(node);
    }

  }

  public static ColorStaticCU getStaticCU(final IRNode node) {
    ColorStaticCU res;
    synchronized (ColorStaticCU.class) {
      res = nodeToCU.get(node);
      if (res != null) {
        return res;
      }
    }
    // note that the locking isn't quite right here!
    ColorFirstPass.getInstance().buildStaticStructureForACU(node);
    synchronized (ColorStaticCU.class) {
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
