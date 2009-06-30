/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/ColorStaticBlock.java,v 1.2 2007/07/09 14:08:28 chance Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import java.util.Set;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.promises.ColorGrantDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ColorRevokeDrop;

@Deprecated
public class ColorStaticBlock extends ColorStaticBlockish {

  public final Set<ColorGrantDrop> grants;
  public final Set<ColorRevokeDrop> revokes;
  
  @Override
  public void accept(IColorVisitor visitor) {
    visitor.visitBlock(this);
  }
  
  public ColorStaticBlock(
      final IRNode node,
      final ColorStaticWithChildren parent,
      final Set<ColorGrantDrop> grants, 
      final Set<ColorRevokeDrop> revokes) {
    super(node, parent);
    this.grants = grants;
    this.revokes = revokes;
  }

}
