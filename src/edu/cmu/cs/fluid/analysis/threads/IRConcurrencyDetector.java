package edu.cmu.cs.fluid.analysis.threads;

import java.util.Iterator;

import org.eclipse.jdt.core.JavaModelException;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.cmu.cs.fluid.analysis.util.AbstractIRAnalysisModule#doAnalysisOnAFile
	 * (edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	protected void doAnalysisOnAFile(IRNode cu) throws JavaModelException {
		if (v == null) {
			// initialize after everything else is setup
			v = new FastVisitor();
		}
		Iterator<IRNode> e = JJNode.tree.bottomUp(cu);
		while (e.hasNext()) {
			v.doAccept(e.next());
		}
	}

	private class FastVisitor extends Visitor<Object> {
		final ITypeEnvironment tEnv  = Eclipse.getDefault().getETypeEnv(getProject());
		final IJavaType threadType   = findNamedType(tEnv, "java.lang.Thread");
		final IJavaType runnableType = findNamedType(tEnv, "java.lang.Runnable");

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