/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/ColorStaticClass.java,v 1.3 2007/07/09 14:08:28 chance Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.promises.ColorImportDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ColorRenameDrop;

@Deprecated
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
