package edu.cmu.cs.fluid.sea;

import com.surelogic.Utility;
import com.surelogic.common.i18n.JavaSourceReference;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.util.VisitUtil;

@Utility
public final class DropSeaUtility {

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
