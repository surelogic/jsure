/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.bca;

import java.util.Iterator;
import java.util.List;

import com.surelogic.analysis.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public class LocalVariablesModule extends AbstractWholeIRAnalysis<IBinderClient, Void> {
	private static final Category LV_CATEGORY = Category.getInstance("LVCategory");	
	
	public LocalVariablesModule() {
		super("LVCategory");
	}
	
	public void init(IIRAnalysisEnvironment env) {
		// Nothing to do
	}

	@Override
	protected IBinderClient constructIRAnalysis(IBinder binder) {
		return null;
	}

	@Override
	protected boolean doAnalysisOnAFile(CUDrop cud, final IRNode compUnit, IAnalysisMonitor monitor) {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				runOverFile(compUnit);
			}
		});
		return true;
	}

	protected void runOverFile(final IRNode compUnit) {
	  final LV_Visitor v = new LV_Visitor();
	  v.doAccept(compUnit);
	}	
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
	
	private final class LV_Visitor extends JavaSemanticsVisitor {
		public LV_Visitor() {
			super(true);
		}
		
		private void reportLocalVariables(final IRNode mdecl) {
			final LocalVariableDeclarations lvd = LocalVariableDeclarations.getDeclarationsFor(mdecl);
			final InfoDrop drop = new InfoDrop();
			setResultDependUponDrop(drop, mdecl);
			drop.setCategory(LV_CATEGORY);
			drop.setMessage("{0}: Local {1}; External {2}", 
					JavaNames.genQualifiedMethodConstructorName(mdecl), 
					listToString(lvd.getLocal()), listToString(lvd.getExternal()));
		}

		@Override
		protected void enteringEnclosingType(final IRNode newType) {
			System.out.println(">>> Entering type " + JavaNames.getTypeName(newType));
		}

		@Override
		protected void leavingEnclosingType(final IRNode newType) {
			System.out.println("<<< Leaving type " + JavaNames.getTypeName(newType));
		}

		@Override 
		protected void enteringEnclosingDecl(
				final IRNode mdecl, final boolean isAnonClassInit) {
			System.out.println("--- Entering method/constructor " + JavaNames.genMethodConstructorName(mdecl));
		}


		@Override 
		protected void leavingEnclosingDecl(final IRNode mdecl) {
			System.out.println("--- Leaving method/constructor " + JavaNames.genMethodConstructorName(mdecl));
		}

		/* Need to override this to return NULL_ACTION so that we process the 
		 * field inits and instance init of anon class expressions in expression
		 * statements.  Also, we need to compute the local variables for the 
		 * anonymous class initializer.
		 */
		@Override
		protected InstanceInitAction getAnonClassInitAction(final IRNode expr) {
			return new InstanceInitAction() {
				public void tryBefore() {
					reportLocalVariables(JavaPromise.getInitMethod(expr));
				}

				public void finallyAfter() {
					// do nothing
				}

				public void afterVisit() {
					// do nothing
				}
			};
		}

		@Override
		protected void handleConstructorDeclaration(final IRNode cdecl) {
			reportLocalVariables(cdecl);
			super.handleConstructorDeclaration(cdecl);
		}

		@Override
		protected void handleMethodDeclaration(final IRNode mdecl) {
			reportLocalVariables(mdecl);
			super.handleMethodDeclaration(mdecl);
		}

		@Override
		protected void handleNonAnnotationTypeDeclaration(final IRNode tdecl) {
			final IRNode clinit = JavaPromise.getClassInitOrNull(tdecl);
			if (clinit != null) {
				reportLocalVariables(clinit);
			}
			super.handleNonAnnotationTypeDeclaration(tdecl);
		}
	}


	private static String listToString(final List<IRNode> list) {
		final StringBuilder sb = new StringBuilder();
		sb.append('[');
		final Iterator<IRNode> i = list.iterator();
		while (i.hasNext()) {
			sb.append(DebugUnparser.toString(i.next()));
			if (i.hasNext()) sb.append(", ");
		}
		sb.append(']');
		return sb.toString();
	}
}
