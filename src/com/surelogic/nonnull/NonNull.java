package com.surelogic.nonnull;

import java.util.Set;
import java.util.logging.Level;

import org.eclipse.core.resources.IProject;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.analysis.util.AbstractWholeIRAnalysisModule;
import edu.cmu.cs.fluid.dc.IAnalysis;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;

public final class NonNull extends AbstractWholeIRAnalysisModule {
	private static final Category NONNULL_CATEGORY = Category
			.getInstance("NonNullCategory");

	static private class ResultsDepDrop extends Drop {
		// Marker class
	}

	private Drop resultDependUpon = null;

	private static NonNull INSTANCE;

	private IBinder binder = null;
	private SimpleNonnullAnalysis nonNullAnalysis = null;

	/**
	 * Provides a reference to the sole object of this class.
	 * 
	 * @return a reference to the only object of this class
	 */
	public static IAnalysis getInstance() {
		return INSTANCE;
	}

	/**
	 * Public constructor that will be called by Eclipse when this analysis
	 * module is created.
	 */
	public NonNull() {
		super(ParserNeed.EITHER);
		INSTANCE = this;
	}

	/**
	 * @see edu.cmu.cs.fluid.dc.IAnalysis#analyzeBegin(org.eclipse.core.resources.IProject)
	 */
	@Override
	public void analyzeBegin(IProject project) {
		super.analyzeBegin(project);

		if (resultDependUpon != null) {
			resultDependUpon.invalidate();
			resultDependUpon = new ResultsDepDrop();
		} else {
			resultDependUpon = new ResultsDepDrop();
		}
	}

	private void setLockResultDep(IRReferenceDrop drop, IRNode node) {
		drop.setNode(node);
		if (resultDependUpon != null && resultDependUpon.isValid()) {
			resultDependUpon.addDependent(drop);
		}
	}

	@Override
	protected void constructIRAnalysis() {
		// FIX temporary -- should be in super class
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				binder = Eclipse.getDefault().getTypeEnv(getProject())
						.getBinder();
				try {
					nonNullAnalysis = new SimpleNonnullAnalysis(
							"Non Null Analysis", binder);
				} catch (SlotAlreadyRegisteredException e) {
					SLLogger.getLogger().log(Level.SEVERE,
							"Problem creating Non Null Analysis", e);
				}
			}
		});
	}

	@Override
	protected void doAnalysisOnAFile(final IRNode compUnit) {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				checkNonNullForFile(compUnit);
			}
		});
	}

	protected void checkNonNullForFile(final IRNode compUnit) {
		/*
		 * Run around the tree looking for variable use expressions.
		 */
		for (IRNode node : JJNode.tree.topDown(compUnit)) {
			final Operator op = JJNode.tree.getOperator(node);
			if (VariableUseExpression.prototype.includes(op)) {
				// See if the current variable is a primitive or not
				final IJavaType type = binder.getJavaType(node);
				if (type instanceof IJavaReferenceType) {
					// See if the current variable is considered to be null or
					// not
					final Set<IRNode> nonNull = nonNullAnalysis
							.getNonnullBefore(node);
					final IRNode varDecl = binder.getBinding(node);
					final InfoDrop drop = new InfoDrop();
					setLockResultDep(drop, node);
					drop.setCategory(NONNULL_CATEGORY);
					final String varName = VariableUseExpression.getId(node);
					if (nonNull.contains(varDecl)) {
						drop.setMessage(varName + " IS NOT null");
					} else {
						drop.setMessage(varName + " may be null");
					}
				}
			}
		}
	}
}