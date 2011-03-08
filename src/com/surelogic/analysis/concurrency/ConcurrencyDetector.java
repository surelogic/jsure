/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.concurrency;

import java.util.Iterator;

import com.surelogic.analysis.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public class ConcurrencyDetector extends AbstractWholeIRAnalysis<ConcurrencyDetector.FastVisitor,IRNode> {
	private static void reportInference(Category c, int number, String arg, IRNode loc) {
		InfoDrop id = new InfoDrop(Messages.toString(number));
		// rd.addCheckedPromise(pd);
		id.setNodeAndCompilationUnitDependency(loc);
		id.setResultMessage(number, arg);
		id.setCategory(c);
	}

	private static IJavaDeclaredType findNamedType(final ITypeEnvironment tEnv, String qname) {
		final IRNode t = tEnv.findNamedType(qname);
		return (IJavaDeclaredType) JavaTypeFactory.convertNodeTypeToIJavaType(
				t, tEnv.getBinder());
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
			tEnv.getBinder().findOverriddenParentMethods(node);
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
				reportInference(Messages.DSC_THREAD_CREATION, Messages.INSTANCE_CREATED, 
						JavaNames.getTypeName(decl), n);
			} else if (implementsRunnable(type)) {
				reportInference(Messages.DSC_RUNNABLE_CREATION, Messages.INSTANCE_CREATED, 
						JavaNames.getTypeName(decl), n);
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
				IJavaType type = JavaTypeFactory.convertNodeTypeToIJavaType(t,
						tEnv.getBinder());
				if (type instanceof IJavaDeclaredType
						&& isThreadStart(m, (IJavaDeclaredType) type)) {
					reportInference(Messages.DSC_THREAD_STARTS, Messages.THREAD_STARTED, 
							JavaNames.getTypeName(t), n);
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
