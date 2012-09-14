/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticClass.java,v 1.4 2007/07/09 13:52:31 chance Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.HashSet;
import java.util.Set;

import com.surelogic.dropsea.ir.drops.promises.threadroles.TRoleImportDrop;
import com.surelogic.dropsea.ir.drops.promises.threadroles.TRoleRenameDrop;

import edu.cmu.cs.fluid.ir.IRNode;


public class TRoleStaticClass extends TRoleStaticWithChildren {

  public Set<TRoleImportDrop> trImports;
  public Set<TRoleRenameDrop> trRenames;
  public Set<TRoleStaticCU> trImportsToRename;
  


  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure#accept(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.IColorVisitor)
   */
  @Override
  public void accept(ITRoleStaticVisitor visitor) {
    visitor.visitClass(this);
  }
  
  public TRoleStaticClass(final IRNode node,
      final TRoleStaticWithChildren parent) {
    super(node, parent);
    trImports = new HashSet<TRoleImportDrop>(1);
    trRenames = new HashSet<TRoleRenameDrop>(1);
    trImportsToRename = new HashSet<TRoleStaticCU>(1);
  }

}
