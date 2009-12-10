/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticBlock.java,v 1.2 2007/07/09 13:39:26 chance Exp $*/
package com.surelogic.analysis.colors;

import java.util.Set;

import com.surelogic.sea.drops.colors.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.colors.ColorGrantDrop;
import edu.cmu.cs.fluid.sea.drops.colors.ColorRevokeDrop;


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
