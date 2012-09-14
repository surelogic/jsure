package edu.cmu.cs.fluid.sea;

import java.util.logging.Level;

import com.surelogic.Utility;
import com.surelogic.common.i18n.JavaSourceReference;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.promise.TextFile;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.tree.Operator;

@Utility
public final class DropSeaUtility {

  /**
   * Utility function that helps build required drop dependencies upon
   * compilation unit drops, it causes this drop to be dependent upon the
   * compilation unit drop that the given fAST node exists within.
   * 
   * @param drop
   *          the drop that should depend upon the compilation unit of the
   *          passed node. If this is <code>null</code> no dependency is added.
   * @param node
   *          the fAST node specifying the compilation unit this drop needs to
   *          depend upon. If this is <code>null</code> no dependency is added.
   */
  public final void dependUponCompilationUnitOf(Drop drop, IRNode node) {
    if (drop == null || node == null)
      return;

    try {
      Operator op = JJNode.tree.getOperator(node);
      IRNode cu;
      if (CompilationUnit.prototype.includes(op)) {
        cu = node;
      } else if (TextFile.prototype.includes(op)) {
        // Not from a compilation unit
        return;
      } else {
        cu = VisitUtil.getEnclosingCompilationUnit(node);
      }
      if (cu == null) {
        SLLogger.getLogger().log(Level.SEVERE, "unable to find enclosing compilation unit for " + DebugUnparser.toString(node));
      } else {
        CUDrop cuDrop = CUDrop.queryCU(cu);
        if (cuDrop == null) {
          IRNode type = VisitUtil.getEnclosingType(node);
          if (!PromiseConstants.ARRAY_CLASS_NAME.equals(JJNode.getInfo(type))) {
            SLLogger.getLogger().log(Level.WARNING, "unable to find compilation unit drop for " + DebugUnparser.toString(node));
          }
        } else {
          /*
           * the promise depends upon the compilation unit it is within
           */
          cuDrop.addDependent(drop);
        }
      }
    } catch (Throwable e) {
      SLLogger.getLogger().log(Level.WARNING, "unable to find compilation unit drop for " + DebugUnparser.toString(node));
    }
  }

  public static JavaSourceReference createJavaSourceReferenceFromOneOrTheOther(IRNode n, ISrcRef ref) {
    if (ref == null) {
      if (n == null) {
        return null;
      }
      IRNode cu = VisitUtil.getEnclosingCUorHere(n);
      String pkg = VisitUtil.getPackageName(cu);
      IRNode type = VisitUtil.getPrimaryType(cu);
      return new JavaSourceReference(pkg, JavaNames.getTypeName(type));
    }
    return new JavaSourceReference(ref.getPackage(), ref.getCUName(), ref.getLineNumber(), ref.getOffset());
  }

  private DropSeaUtility() {
    // no instances
  }

}
