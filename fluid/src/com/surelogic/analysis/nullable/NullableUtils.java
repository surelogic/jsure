package com.surelogic.analysis.nullable;

import com.surelogic.aast.promise.CastNode.CastKind;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.PackageDeclaration;
import edu.cmu.cs.fluid.java.operator.TypeDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;

final class NullableUtils {
  private NullableUtils() {
    // Utility class
  }

  public enum Method {
    TO_NON_NULL {
      @Override
      public CastKind getCastKind() { return CastKind.toNonNull; }
    },
    
    TO_NULLABLE {
      @Override
      public CastKind getCastKind() { return CastKind.toNullable; }
    };
    
    public abstract CastKind getCastKind();
  }

  /**
   * Is the given method declaration a special nullable cast method, and if
   * so, which one?
   * 
   * @return <code>null</code> if the <code>mdecl</code> is not a
   * MethodDeclaration node, or if the method is not a cast method.
   * Returns {@link Method#TO_NON_NULL} if the method is a the method
   * that casts to non null, and {@link Method#TO_NULLABLE} if the method
   * is the method that casts to nullable.
   */
  public static Method isCastMethod(final IRNode mdecl) {
    if (!MethodDeclaration.prototype.includes(mdecl)) {
      return null;
    }

    final IRNode typeDecl = VisitUtil.getEnclosingType(mdecl);
    if (!AnonClassExpression.prototype.includes(typeDecl) &&
        TypeDeclaration.getId(typeDecl).equals("Cast")) {
      final IRNode cuDecl = VisitUtil.getEnclosingCompilationUnit(typeDecl);
      if (PackageDeclaration.getId(
          CompilationUnit.getPkg(cuDecl)).equals("com.surelogic")) {
        final String name = MethodDeclaration.getId(mdecl);
        if (name.equals("toNonNull")) {
          return Method.TO_NON_NULL;
        } else if (name.equals("toNullable")) {
          return Method.TO_NULLABLE;
        }
      }
    }
    return null;
  }
}
