package com.surelogic.analysis.testing;

import com.surelogic.analysis.*;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.parse.JJNode;

public final class TypeBasedAliasModule extends AbstractWholeIRAnalysis<IBinderClient, Unused> {
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
			@Override
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
		        final HintDrop drop = HintDrop.newInformation(decl);
		        drop.setCategorizingMessage(Messages.DSC_TEST_ALIAS);
		        drop.setMessage(Messages.ALIASED_PARAMETERS,
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
