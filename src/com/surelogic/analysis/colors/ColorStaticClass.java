/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticClass.java,v 1.4 2007/07/09 13:52:31 chance Exp $*/
package com.surelogic.analysis.colors;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.colors.ColorImportDrop;
import edu.cmu.cs.fluid.sea.drops.colors.ColorRenameDrop;


public class ColorStaticClass extends ColorStaticWithChildren {

  public Set<ColorImportDrop> colorImports;
  public Set<ColorRenameDrop> colorRenames;
  public Set<ColorStaticCU> colorImportsToRename;
  


  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure#accept(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.IColorVisitor)
   */
  @Override
  public void accept(IColorVisitor visitor) {
    visitor.visitClass(this);
  }
  
  public ColorStaticClass(final IRNode node,
      final ColorStaticWithChildren parent) {
    super(node, parent);
    colorImports = new HashSet<ColorImportDrop>(1);
    colorRenames = new HashSet<ColorRenameDrop>(1);
    colorImportsToRename = new HashSet<ColorStaticCU>(1);
  }

}
