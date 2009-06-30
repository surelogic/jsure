/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticRef.java,v 1.6 2007/07/10 22:16:34 aarong Exp $*/
package com.surelogic.analysis.colors;

import java.util.*;

import com.surelogic.sea.drops.colors.ColorizedRegionModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;


public class ColorStaticRef extends ColorStaticStructure {

  public List<ColorizedRegionModel> colorTargetsHere;
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure#accept(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.IColorVisitor)
   */
  @Override
  public void accept(IColorVisitor visitor) {
    visitor.visitReference(this);
  }
  
  public ColorStaticRef(final IRNode node, ColorStaticBlockish parent, final List<ColorizedRegionModel> tgts) {
    super(node, parent);
    final Operator op = JJNode.tree.getOperator(node);
    parent.allRefs.add(this);
    if (tgts != null && !tgts.isEmpty()) {
      parent.interestingRefs.add(this);
    }
    colorTargetsHere = tgts;
  }

}
