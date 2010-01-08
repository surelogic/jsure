/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/DeclaredPermissionIntentVisitor.java,v 1.4 2007/07/10 22:16:29 aarong Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import java.util.Iterator;

import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.UniquenessAnnotation;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.parse.JJNode;

@Deprecated
public class DeclaredPermissionIntentVisitor extends VoidTreeWalkVisitor {

  private boolean declaredIntent;
  private final IBinder binder;
  
  public DeclaredPermissionIntentVisitor(IBinder b) {
    super();
    binder = b;
  }
  
  public boolean usesDeclaredIntent(IRNode n){
    declaredIntent = false;
    // XXX: This is broken: We should take the constructorContext into account
    this.doAccept(IntraproceduralAnalysis.getRawFlowUnit(n));
    return declaredIntent;
  }

  
  
  @Override
  public Void visitConstructorCall(IRNode node) {
    IRNode cdecl = binder.getBinding(node);
    declaredIntent |= this.methodHasDeclaredIntent(cdecl);
    return super.visitConstructorCall(node);
  }

  @Override
  public Void visitConstructorDeclaration(IRNode node) {
    declaredIntent |= this.methodHasDeclaredIntent(node);
    return super.visitConstructorDeclaration(node);
  }

  @Override
  public Void visitFieldDeclaration(IRNode node) {
    declaredIntent |= this.fieldHasDeclaredIntent(node);
    return super.visitFieldDeclaration(node);
  }

  @Override
  public Void visitFieldRef(IRNode node) {
    final IRNode fdecl = binder.getBinding(node);
    declaredIntent |= this.fieldHasDeclaredIntent(fdecl);
    return super.visitFieldRef(node);
  }

  @Override
  public Void visitMethodCall(IRNode node) {
    final IRNode mdecl = binder.getBinding(node);
    declaredIntent |= this.methodHasDeclaredIntent(mdecl);
    return super.visitMethodCall(node);
  }

  @Override
  public Void visitMethodDeclaration(IRNode node) {
    declaredIntent |= this.methodHasDeclaredIntent(node);
    return super.visitMethodDeclaration(node);
  }

  @Override
  public Void visitPolymorphicConstructorCall(IRNode node) {
    final IRNode mdecl = binder.getBinding(node);
    declaredIntent |= this.methodHasDeclaredIntent(mdecl);
    return super.visitPolymorphicConstructorCall(node);
  }

  @Override
  public Void visitPolymorphicMethodCall(IRNode node) {
    final IRNode mdecl = binder.getBinding(node);
    declaredIntent |= this.methodHasDeclaredIntent(mdecl);
    return super.visitPolymorphicMethodCall(node);
  }

  protected boolean methodHasDeclaredIntent(IRNode mdecl){
    if(mdecl == null) return false;
/*    if(EffectsAnnotation.hasDeclaredEffects(mdecl)){
      System.err.println(DebugUnparser.toString(mdecl) + " has declared intent.");
      return true;
    }
    */
    final IRNode param = 
      MethodDeclaration.prototype.includes(JJNode.tree.getOperator(mdecl))
    ? MethodDeclaration.getParams(mdecl)
    : ConstructorDeclaration.getParams(mdecl);
    final Iterator<IRNode> params = Parameters.getFormalIterator(param);
    while(params.hasNext()){
      if(parameterHasDeclaredIntent(params.next())) return true;
    }
    if (!TypeUtil.isStatic(mdecl) 
        && parameterHasDeclaredIntent(JavaPromise.getReceiverNode(mdecl))){
      return true;
    }
    return parameterHasDeclaredIntent(JavaPromise.getReturnNodeOrNull(mdecl)); 
  }
  protected boolean fieldHasDeclaredIntent(IRNode fdecl){
    boolean b =  fdecl != null && (UniquenessAnnotation.isUnique(fdecl)
      || UniquenessAnnotation.isImmutable(fdecl));
    if(b) System.err.println(DebugUnparser.toString(fdecl) + " has declared intent");
    return b;
  }
  protected boolean parameterHasDeclaredIntent(IRNode decl){
    boolean b = decl != null && (UniquenessAnnotation.isBorrowed(decl) || 
      UniquenessAnnotation.isUnique(decl) ||
      UniquenessAnnotation.isImmutable(decl));
    if(b) System.err.println(DebugUnparser.toString(decl) + " has declared intent");
    return b;
  }
}
