package edu.cmu.cs.fluid.analysis.threads;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaModelException;

import com.surelogic.analysis.IAnalysisMonitor;

import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
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

public final class IRConcurrencyDetector extends AbstractConcurrencyDetector {
	private static final IRConcurrencyDetector INSTANCE = new IRConcurrencyDetector();

	public static IRConcurrencyDetector getInstance() {
		return INSTANCE;
	}

	public IRConcurrencyDetector() {
	}

	FastVisitor v = null;

	@Override
	public void analyzeBegin(IProject p) {
		super.analyzeBegin(p);
		
		// initialize after everything else is setup
		v = new FastVisitor(Eclipse.getDefault().getETypeEnv(p));		
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule#doAnalysisOnAFile
	 * (edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	protected boolean doAnalysisOnAFile(IRNode cu, IAnalysisMonitor monitor) throws JavaModelException {
		Iterator<IRNode> e = JJNode.tree.bottomUp(cu);
		while (e.hasNext()) {
			v.doAccept(e.next());
		}
		return true;
	}

	private class FastVisitor extends Visitor<Object> {
		final ITypeEnvironment tEnv;
		final IJavaType threadType;
		final IJavaType runnableType;

		FastVisitor(ITypeEnvironment te) {
			tEnv = te;
			threadType = findNamedType(tEnv, "java.lang.Thread");
			runnableType = findNamedType(tEnv, "java.lang.Runnable");
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
				reportInference(threadCreationCategory, JavaNames
						.getTypeName(decl)
						+ " instance created", n);
			} else if (implementsRunnable(type)) {
				reportInference(runnableCreationCategory, JavaNames
						.getTypeName(decl)
						+ " instance created", n);
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
					reportInference(threadStartsCategory, JavaNames
							.getTypeName(t)
							+ " started", n);
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