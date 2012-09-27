package com.surelogic.analysis.concurrency.detector;

import java.util.Iterator;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.Unused;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

public class ConcurrencyDetector extends AbstractWholeIRAnalysis<ConcurrencyDetector.FastVisitor, Unused> {
  private void reportInference(IRNode loc, int catNumber, int resNumber, Object... args) {
    HintDrop id = HintDrop.newInformation(loc);
    id.setMessage(resNumber, args);
    id.setCategorizingString(catNumber);
  }

  private static IJavaDeclaredType findNamedType(final ITypeEnvironment tEnv, String qname) {
    final IRNode t = tEnv.findNamedType(qname);
    return (IJavaDeclaredType) JavaTypeFactory.convertNodeTypeToIJavaType(t, tEnv.getBinder());
  }

  public ConcurrencyDetector() {
    super("ConcurrencyDetector");
  }

  @Override
  public void init(IIRAnalysisEnvironment env) {
    super.init(env);
    env.ensureClassIsLoaded("java.lang.Runnable");
    env.ensureClassIsLoaded("java.lang.Thread");
  }

  @Override
  protected void clearCaches() {
    // Nothing to do?
  }

  @Override
  protected FastVisitor constructIRAnalysis(IBinder binder) {
    return new FastVisitor(binder.getTypeEnvironment());
  }

  @Override
  protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
    Iterator<IRNode> e = JJNode.tree.bottomUp(cu);
    while (e.hasNext()) {
      getAnalysis().doAccept(e.next());
    }
    return true;
  }

  @Override
  public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
    finishBuild();
    return super.analyzeEnd(env, p);
  }

  class FastVisitor extends Visitor<Object> implements IBinderClient {
    public void clearCaches() {
      // Nothing to do
    }

    public IBinder getBinder() {
      // TODO Auto-generated method stub
      return null;
    }

    final ITypeEnvironment tEnv;
    final IJavaType threadType;
    final IJavaType runnableType;

    FastVisitor(ITypeEnvironment te) {
      tEnv = te;
      threadType = findNamedType(tEnv, "java.lang.Thread");
      runnableType = findNamedType(tEnv, "java.lang.Runnable");
    }

    @Override
    public Object visitMethodDeclaration(IRNode node) {
      return null;
    }

    // TODO other forms?
    @Override
    public Object visitNewExpression(IRNode n) {
      IJavaType t = tEnv.getBinder().getJavaType(n);
      if (t == null) {
        return null;
      }
      if (!(t instanceof IJavaDeclaredType)) {
        return null;
      }
      IJavaDeclaredType type = (IJavaDeclaredType) t;
      IRNode decl = type.getDeclaration();
      if (isThreadSubtype(type)) {
        reportInference(n, 50, 50, JavaNames.getTypeName(decl));
      } else if (implementsRunnable(type)) {
        reportInference(n, 51, 50, JavaNames.getTypeName(decl));
      }
      return null;
    }

    @Override
    public Object visitMethodCall(IRNode n) {
      final String name = MethodCall.getMethod(n);
      if (name.equals("start")) {
        IRNode m = tEnv.getBinder().getBinding(n);
        if (m == null) {
          return null;
        }
        IRNode t = VisitUtil.getEnclosingType(m);
        if (t == null) {
          return null;
        }
        IJavaType type = JavaTypeFactory.convertNodeTypeToIJavaType(t, tEnv.getBinder());
        if (type instanceof IJavaDeclaredType && isThreadStart(m, (IJavaDeclaredType) type)) {
          reportInference(n, 52, 51, JavaNames.getTypeName(t));
        }
      }
      return null;
    }

    private boolean isThreadStart(IRNode method, IJavaDeclaredType type) {
      // must be the non-static start method
      if (JavaNode.getModifier(method, JavaNode.STATIC)) {
        return false;
      }
      // must be the no-arg start method
      IRNode params = MethodDeclaration.getParams(method);
      if (Parameters.getFormalIterator(params).hasNext()) {
        return false;
      }
      return implementsRunnable(type) || isThreadSubtype(type);
    }

    private boolean implementsRunnable(IJavaDeclaredType type) {
      return tEnv.isSubType(type, runnableType);
    }

    private boolean isThreadSubtype(IJavaDeclaredType type) {
      return tEnv.isSubType(type, threadType);
    }
  }
}
