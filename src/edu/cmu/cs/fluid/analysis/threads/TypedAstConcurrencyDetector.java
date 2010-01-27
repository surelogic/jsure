package edu.cmu.cs.fluid.analysis.threads;

import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.IAnalysisMonitor;
import com.surelogic.ast.*;
import com.surelogic.ast.java.operator.*;

import edu.cmu.cs.fluid.ir.IRNode;

public final class TypedAstConcurrencyDetector extends AbstractConcurrencyDetector {
  private static final TypedAstConcurrencyDetector INSTANCE = new TypedAstConcurrencyDetector();
  
  private FastVisitor v = null;

  public static TypedAstConcurrencyDetector getInstance() {
    return INSTANCE;
  }
  
  private TypedAstConcurrencyDetector() {
  }

  @Override
  protected boolean useTypedASTs() {
    return true;
  }

  @Override
  protected boolean doAnalysisOnAFile(IRNode cu, IAnalysisMonitor monitor) throws JavaModelException {
    throw new UnsupportedOperationException();
  }
  
  @Override
  protected boolean doAnalysisOnAFile(IRNode n, ICompilationUnitNode cu, IAnalysisMonitor monitor)
  throws JavaModelException 
  {     
    if (cu == null) {
//      System.out.println("cu is null");
      return false;
    }
    if (v == null) {
      v = new FastVisitor(); 
    }
    /*
    Iterator<IRNode> e = JavaNode.tree.bottomUp(n);
    while (e.hasNext()) {
      v.doAccept(e.next());
    }
    */
    return false;
  }

  private class FastVisitor extends AbstractingBaseVisitor<Void> {
    final ISourceRefType threadType = findNamedTypeBinding("java.lang.Thread");    
    final ISourceRefType runnableType = findNamedTypeBinding("java.lang.Runnable");

    // TODO other forms?
    @Override
    public Void visit(INewExpressionNode n) {
      IType tb = n.resolveType();
      if (tb == null) {
        return null;
      }      
      if (!(tb instanceof ISourceRefType)) {
        return null;
      }
      ISourceRefType t = (ISourceRefType) tb;
      IDeclarationNode decl   = t.getNode();
      if (isThreadSubtype(t)) {
        reportInference(threadCreationCategory, 50, getDeclName(decl), toNode(n));
      } else if (implementsRunnable(t)) {
        reportInference(runnableCreationCategory, 50, getDeclName(decl), toNode(n));
      }
      return null;
    }

    @Override
    public Void visit(IMethodCallNode n) {
      final String name = n.getMethod();
      if (name.equals("start")) {
        IMethodBinding mb = n.resolveBinding();
        if (mb == null) {
          return null;
        }

        if (isThreadStart(mb)) {
          String typeName = getDeclName(mb.getContextType().getNode());
          reportInference(threadStartsCategory, 51, typeName, toNode(n));
        }
      }
      return null;
    }

    private boolean isThreadStart(IMethodBinding mb) {
      IMethodDeclarationNode m = mb.getNode();
      
      // must be the non-static start method
      if (m.isStatic()) {
        return false;
      }
      // must be the no-arg start method
      if (m.getParamsList().size() != 0) {
        return false;
      }
      ISourceRefType type = mb.getContextType();
      return implementsRunnable(type) || isThreadSubtype(type);
    }

    private boolean implementsRunnable(ISourceRefType type) {
      return runnableType.isSubtypeOf(type);
    }

    private boolean isThreadSubtype(ISourceRefType type) {
      return threadType.isSubtypeOf(type);
    }
  }
}