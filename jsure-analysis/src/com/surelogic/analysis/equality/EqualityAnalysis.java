package com.surelogic.analysis.equality;

import java.util.List;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.annotationbounds.ParameterizedTypeAnalysis;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.targets.ClassTarget;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.evidence.NoEvidence;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.analysis.type.constraints.TypeAnnotationTester;
import com.surelogic.analysis.type.constraints.TypeAnnotations;
import com.surelogic.annotation.rules.EqualityRules;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ProofDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.type.constraints.RefObjectPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.BinopExpression;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.parse.JJNode;

public final class EqualityAnalysis extends AbstractWholeIRAnalysis<EqualityAnalysis.PerThreadInfo,CUDrop> {
  private static final int BAD_COMPARISON = 758;
  private static final int TO_STRING_GOOD = 759;
  private static final int TO_STRING_BAD = 760;
  private static final int WRITE_EFFECT_INFO = 761;
  private static final int READ_EFFECT_WARN = 762;
  private static final int UNANNOTATED = 766;
  
  
  
	public EqualityAnalysis() {
		super("Equality");
	}
	
	@Override
	protected boolean flushAnalysis() {
		return true;
	}
	
	@Override
	protected void clearCaches() {
		// Nothing to do, because of flushAnalysis
	}
	
	@Override
	protected PerThreadInfo constructIRAnalysis(IBinder binder) {
		return new PerThreadInfo(binder);
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
		//System.out.println("Analyzing equality for: "+cud.javaOSFileName);
	  final PerThreadInfo analysis = getAnalysis();
	  analysis.initForCU(cu);
		analysis.doAccept(cu);
		return true;
	}
	
	@Override
	public Iterable<CUDrop> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		finishBuild();
		return super.analyzeEnd(env, p);
	}
	
	ResultDrop createFailureDrop(IRNode n) {
		ResultDrop rd = new ResultDrop(n);
		rd.setCategorizingMessage(Messages.DSC_LAYERS_ISSUES);
		rd.setInconsistent();
		return rd;
	}
	
	final class PerThreadInfo extends VoidTreeWalkVisitor implements IBinderClient {
		final IBinder b;
		private Effect readsAnything;
		private RegionModel instanceRegion;
		
		
		
		PerThreadInfo(IBinder binder) {
			b = binder;
		}

		@Override
		public IBinder getBinder() {
			return b;
		}

		@Override
		public void clearCaches() {
			// Nothing to do yet
		}
		
		
		void initForCU(final IRNode cu) {
      final Target anything = 
          new ClassTarget(RegionModel.getAllRegion(cu), NoEvidence.INSTANCE);
      readsAnything = Effect.read(cu, anything, Effect.NO_LOCKS);      
      instanceRegion = RegionModel.getInstanceRegion(cu);
		}
		
		
		@Override
		public Void visitMethodDeclaration(final IRNode mdecl) {
		  /* We do not have to worry about the case where the class inherits the
		   * implementation of toString().  If the class inherits the
		   * implementation from java.lang.Object, it is safe.  Otherwise,
		   * the class must extend from a @ReferenceObject class, and the superclass's
		   * implementation is checked.
		   */
		  
		  // See of we are "void toString()" in a @ReferenceObject class
		  final IRNode cdecl = JJNode.tree.getParent(JJNode.tree.getParent(mdecl));
		  final RefObjectPromiseDrop refObj = EqualityRules.getRefObjectDrop(cdecl);
		  if (refObj != null && 
		      MethodDeclaration.getId(mdecl).equals("toString") &&
		      JJNode.tree.numChildren(MethodDeclaration.getParams(mdecl)) == 0) {
		    // Get the declared effects of the method
		    final List<Effect> declared =
		        Effects.getDeclaredMethodEffects(mdecl, null);

        final ResultDrop result = new ResultDrop(mdecl);
//        result.setMessagesByJudgement(TO_STRING_GOOD, TO_STRING_BAD);
        result.addChecked(refObj);
        
		    boolean good = true;
		    if (declared == null) {
		      ResultsBuilder.createResult(true, result, mdecl, UNANNOTATED);
		      good = false;
		    } else {
		      // add effects promise to the results
		      result.addTrusted(MethodEffectsRules.getRegionEffectsDrop(mdecl));
		      
		      for (final Effect de : declared) {
		        if (!de.isCheckedBy(getBinder(), readsAnything)) {
		          good = false;
		          result.addInformationHint(mdecl, WRITE_EFFECT_INFO, de.unparseForMessage());
		        }
		      }

	        if (good) {
	          // Check for too may read effects
	          final Effect readsThisInstance = 
	              Effect.read(null,
	                  new InstanceTarget(
	                      JavaPromise.getReceiverNode(mdecl),
	                      instanceRegion, NoEvidence.INSTANCE),
	                  Effect.NO_LOCKS);
	          for (final Effect de : declared) {
	            if (!de.isCheckedBy(getBinder(), readsThisInstance)) {
	              result.addWarningHint(mdecl, READ_EFFECT_WARN, de.unparseForMessage());
	            }
	          }
	        }
		    }
        result.setConsistent(good);
        /* Don't use setMessagesByJudgment because I don't want the message
         * to change if the result is indirectly bad.
         */
        result.setMessage(good ? TO_STRING_GOOD : TO_STRING_BAD);
		  }
		  return null;
		}
		
		@Override
		public Void visitEqualityExpression(IRNode node) {
			final IRNode e1 = BinopExpression.getOp1(node);
			final IRNode e2 = BinopExpression.getOp2(node);
			if (NullLiteral.prototype.includes(e1) || NullLiteral.prototype.includes(e2)) {
				// It's ok to compare to null 
				return null;
			}
			checkIfValueObject(e1);
			checkIfValueObject(e2);
			return null;
		}
		
		@SuppressWarnings("unchecked")
    void checkIfValueObject(IRNode e) {
			IJavaType t = b.getJavaType(e);
			final TypeAnnotationTester tester =
			    new TypeAnnotationTester(TypeAnnotations.VALUE_OBJECT, b,
			        ParameterizedTypeAnalysis.getFolders());
			if (tester.testExpressionType(t)) {
				ResultDrop d = createFailureDrop(e);
				for (final ProofDrop p : tester.getTrusts()) {
				  if (p instanceof PromiseDrop) {
				    d.addChecked((PromiseDrop<? extends IAASTRootNode>) p);
				  }
				}
				d.setMessage(BAD_COMPARISON, DebugUnparser.toString(e));
			}
		}
	}
}
