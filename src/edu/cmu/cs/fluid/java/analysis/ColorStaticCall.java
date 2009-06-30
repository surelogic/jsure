/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/ColorStaticCall.java,v 1.4 2007/07/10 22:16:29 aarong Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import java.util.List;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.promises.ColorizedRegionModel;

@Deprecated
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
