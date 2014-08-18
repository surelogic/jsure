package com.surelogic.analysis.nullable;

import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.CastNode.CastKind;
import com.surelogic.aast.promise.NonNullNode;
import com.surelogic.aast.promise.NullableNode;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.NullablePromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.PackageDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

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
        JJNode.getInfo(typeDecl).equals("Cast")) {
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
  
  public static NonNullPromiseDrop attachVirtualNonNull(
      final IRNode decl, final Set<PromiseDrop<?>> created) {
    final NonNullNode nn = new NonNullNode(0);
    nn.setPromisedFor(decl, null);
    return attachAsVirtual(NonNullRules.getNonNullStorage(),
        new NonNullPromiseDrop(nn), created);
  }
  
  public static NullablePromiseDrop attachVirtualNullable(
      final IRNode decl, final Set<PromiseDrop<?>> created) {
    final NullableNode nn = new NullableNode(0);
    nn.setPromisedFor(decl, null);
    return attachAsVirtual(NonNullRules.getNullableStorage(),
        new NullablePromiseDrop(nn), created);
  }
  
  /**
   * Attach a new virtual annotation to a node.  If successful, the new annotation
   * is added to the collection of virtual annotations.  If the annotation
   * already exists, the existing one is returned.
   */
  /* Synchronized to prevent generation of duplicate virtual promises during
   * concurrent execution.  The synchronization is really protecting the call
   * to AnnotationRules.attachAsVirtual.
   */
  private synchronized static <A extends IAASTRootNode, T extends PromiseDrop<? super A>> T
  attachAsVirtual(final IPromiseDropStorage<T> storage, final T drop,
      Set<PromiseDrop<?>> created) {
    try {
      AnnotationRules.attachAsVirtual(storage, drop);
      created.add(drop);
      return drop;
    } catch (IllegalArgumentException e) {
      // Assumed to be already created
      drop.invalidate();
      return drop.getNode().getSlotValue(storage.getSlotInfo());
      // return storage.getDrops(drop.getNode()).iterator().next();
    }
  }

}
