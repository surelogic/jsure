package edu.cmu.cs.fluid.analysis.threads;

import java.util.logging.Logger;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.analysis.util.AbstractFluidAnalysisModule;

@Deprecated
public final class ConcurrencyDetector extends AbstractFluidAnalysisModule
{
  private static final Logger LOG = SLLogger.getLogger("analysis.threads");

  //private static final String CONCURRENCY_DETECTOR = "Concurrency detector";

  private static class ClassInstanceCreationVisitor extends ASTVisitor {

    @Override
    public boolean visit(ClassInstanceCreation node) {
      ITypeBinding tb = node.resolveTypeBinding();
      //get the type binding for this expression
      if (tb == null) {
//        System.out.println("Empty Type Binding");
        return false;
      }
      if (isThreadSubtype(tb)) {
        // reportInference("java.lang.Thread subtype instance creation(s)", tb
        // .getQualifiedName()
        // + " instance created", CONCURRENCY_DETECTOR, f_ast.lineNumber(node
        // .getStartPosition()));
      }
      if (implementsRunnable(tb) && !isThreadSubtype(tb)) {
        // reportInference(
        // "java.lang.Runnable subtype (not also a Thread subtype) instance
        // creation(s)",
        // tb.getQualifiedName() + " instance created", CONCURRENCY_DETECTOR,
        // f_ast.lineNumber(node.getStartPosition()));
      }
      return true;
    }

    @Override
    public boolean visit(MethodInvocation node) {
      if (node.getName().getIdentifier().equals("start")) {
        IMethodBinding mb = node.resolveMethodBinding();
        if (mb == null)
          return true;
        ITypeBinding tb = mb.getDeclaringClass();
        if (tb == null)
          return true;
        if (isThreadStart(mb, tb)) {
          // reportInference("thread start(s)",
          // tb.getQualifiedName() + " started", CONCURRENCY_DETECTOR, f_ast
          // .lineNumber(node.getStartPosition()));
        }
      }
      // TODO Auto-generated method stub
      return true;
    }

    private boolean isThreadStart(IMethodBinding mb, ITypeBinding tb) {
      // must be the non-static start method
      if (Modifier.isStatic(mb.getModifiers()))
        return false;
      // must be the no-arg start method
      if (mb.getParameterTypes().length != 0)
        return false;
      return implementsRunnable(tb) || isThreadSubtype(tb);
    }

    private boolean implementsRunnable(ITypeBinding tb) {
      if (tb == null)
        return false;
      if (implementsRunnable(tb.getInterfaces())) {
        return true;
      }
      if (tb.equals(AST.newAST(AST.JLS3).resolveWellKnownType("java.lang.Object")))
        return false;
      return implementsRunnable(tb.getSuperclass());
    }

    private boolean isThreadSubtype(ITypeBinding tb) {
      if (tb == null)
        return false;
      if (tb.getQualifiedName().equals("java.lang.Thread"))
        return true;
      if (tb.equals(AST.newAST(AST.JLS3).resolveWellKnownType("java.lang.Object")))
        return false;
      return isThreadSubtype(tb.getSuperclass());
    }

    private boolean implementsRunnable(ITypeBinding[] tb) {
      boolean b = false;
      for (int i = 0; i < tb.length && !b; i++) {
        if (tb[i].getQualifiedName().equals("java.lang.Runnable"))
          b = true;
        else
          b = b || implementsRunnable(tb[i].getInterfaces());
      }
      return b;
    }
  }

  private static final ConcurrencyDetector INSTANCE = new ConcurrencyDetector();

  public static ConcurrencyDetector getInstance() {
    return INSTANCE;
  }
  @Override
  public boolean needsAST() {
    return true;
  }
  @Override
  public boolean analyzeCompilationUnit(ICompilationUnit file, CompilationUnit ast, 
          IAnalysisMonitor monitor) {
    LOG.info("analyzeCompilationUnit() on " + file.getElementName());
    javaFile = file;
    ast.accept(new ClassInstanceCreationVisitor()); // do the actual search
    javaFile = null;
    return true;
  }
  
  @Override
  protected void removeResource(IResource resource) {
    // Nothing to do
  }
}