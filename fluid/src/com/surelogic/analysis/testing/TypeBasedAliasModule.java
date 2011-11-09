package com.surelogic.analysis.testing;

import com.surelogic.analysis.*;
import com.surelogic.analysis.alias.TypeBasedMayAlias;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public final class TypeBasedAliasModule extends AbstractWholeIRAnalysis<IBinderClient, Void> {
	public TypeBasedAliasModule() {
		super("TypeBasedAliasCategory");
	}

	@Override
	protected IBinderClient constructIRAnalysis(final IBinder binder) {
		return null;
	}

	@Override
	protected boolean doAnalysisOnAFile(
	    final IIRAnalysisEnvironment env, final CUDrop cud, final IRNode compUnit) {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				runOverFile(compUnit);
			}
		});
		return true;
	}

	protected void runOverFile(final IRNode compUnit) {
	  final TestAliasesVisitor v = new TestAliasesVisitor();
	  v.doAccept(compUnit);
	}	
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
	
	private final class TestAliasesVisitor extends JavaSemanticsVisitor {
	  final TypeBasedMayAlias alias;
	  
		public TestAliasesVisitor() {
			super(true);
			alias = new TypeBasedMayAlias(getBinder());
		}

		private void testParameterAliases(final IRNode decl) {
		  final IRNode formals = MethodDeclaration.getParams(decl);
		  final int numFormals = JJNode.tree.numChildren(formals);
		  final IRNode[] exprs = new IRNode[numFormals];
		  
		  for (int i = 0; i < numFormals; i++) {
		    exprs[i] = Parameters.getFormal(formals, i);
		  }
		  
		  for (int i = 0; i < numFormals - 1; i++) {
		    for (int j = i + 1; j < numFormals; j++) {
		      if (alias.mayAlias(exprs[i], exprs[j])) {
		        final InfoDrop drop = new InfoDrop(null);
		        setResultDependUponDrop(drop, decl);
		        drop.setCategory(Messages.DSC_TEST_ALIAS);
		        drop.setResultMessage(Messages.ALIASED_PARAMETERS,
		            ParameterDeclaration.getId(exprs[i]),
		            ParameterDeclaration.getId(exprs[j]));
		      }
		    }
		  }
		}

		@Override
		protected void handleMethodDeclaration(final IRNode mdecl) {
		  testParameterAliases(mdecl);
			super.handleMethodDeclaration(mdecl);
		}
	}
}
