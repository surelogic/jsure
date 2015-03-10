package com.surelogic.dropsea.ir.drops;

import com.surelogic.dropsea.ir.AbstractSeaConsistencyProofHook;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.parse.JJNode;

public class NonNullModelClearOutUnusedVirtualProofHook extends AbstractSeaConsistencyProofHook {

  @Override
  public void preConsistencyProof(final Sea sea) {
    /* Clear out virtual NonNull annotations that are on fields that aren't
     * used in a proof.  These are annotations that are added to fields
     * because they are initialized to new objects or string literals.  But 
     * these annotations just clutter up the results if they aren't actually
     * relied on by anything.
     */
    for (final NonNullPromiseDrop nonNull : sea.getDropsOfType(NonNullPromiseDrop.class)) {
      if (nonNull.isVirtual()) {
        final IRNode promisedFor = nonNull.getPromisedFor();
        if (VariableDeclarator.prototype.includes(promisedFor) &&
            FieldDeclaration.prototype.includes(JJNode.tree.getParent(JJNode.tree.getParent(promisedFor)))) {
          if (!nonNull.hasDeponents()) {
            boolean onlyFolders = true;
            for (final Drop d : nonNull.getDependents()) {
              if (!(d instanceof ResultFolderDrop)) onlyFolders = false;
            }
            if (onlyFolders) {
              nonNull.invalidate();
            }
          }
        }
      }
    }
  }
}
