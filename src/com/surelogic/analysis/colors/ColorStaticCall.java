/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticCall.java,v 1.5 2007/07/09 14:26:57 chance Exp $*/
package com.surelogic.analysis.colors;

import java.util.*;


import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.colors.ColorizedRegionModel;


public class ColorStaticCall extends ColorStaticStructure {

  public List<ColorizedRegionModel> colorCRMsHere;
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure#accept(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.IColorVisitor)
   */
  @Override
  public void accept(IColorVisitor visitor) {
     visitor.visitCall(this);
  }
  
  public ColorStaticCall(final IRNode node, final ColorStaticWithChildren parent) {
    super(node, parent);
  }

}
