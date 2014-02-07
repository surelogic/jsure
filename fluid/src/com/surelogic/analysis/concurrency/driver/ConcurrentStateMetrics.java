package com.surelogic.analysis.concurrency.driver;

import com.surelogic.analysis.concurrency.heldlocks.LockUtils;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.ir.MetricDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.VouchFieldIsPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * Scans classes to compute metrics about what kinds of fields we have in each class
 * 
 * @author edwin
 */
public class ConcurrentStateMetrics {
	final IBinder binder;
	
	ConcurrentStateMetrics(IBinder b) {
		binder = b;
	}
	
	void summarizeFieldInfo(final IRNode typeDecl, final IRNode typeBody, LockUtils lockUtils) {		
		final FieldCounts counts = new FieldCounts(typeDecl, lockUtils);
		for(final IRNode field : VisitUtil.getClassFieldDecls(typeDecl)) {
			final IRNode type = FieldDeclaration.getType(field);			
			final IJavaType jt = binder.getJavaType(type);
			if (jt instanceof IJavaSourceRefType) {				
				final IJavaSourceRefType st = (IJavaSourceRefType) jt;
				counts.incrForRef(field, st);
			} 
			else if (jt instanceof IJavaPrimitiveType) {
				counts.incrForPrim(field);
			}
			else if (jt instanceof IJavaArrayType) {
				counts.incrForArray(field);
			}
			else throw new IllegalStateException();
		}
		counts.recordAsDrop();
	}

	class FieldCounts {
		final IJavaDeclaredType clazz;
		final LockUtils lockUtils;
		int threadSafe = 0, immutable = 0, locked = 0, threadConfined = 0, other = 0;

		FieldCounts(IRNode t, LockUtils utils) {
			clazz = (IJavaDeclaredType) binder.getTypeEnvironment().convertNodeTypeToIJavaType(t);
			lockUtils = utils;
		}

		void incrForArray(IRNode field) {
			handleEachDecl(field, false);
		}

		void incrForRef(final IRNode field, final IJavaSourceRefType st) {
			if (LockRules.isImmutableType(st.getDeclaration())) {
				immutable += numDecls(field);
			}
			else if (LockRules.isThreadSafeType(st.getDeclaration())) {
				threadSafe += numDecls(field);
			}
			else handleEachDecl(field, false);
		}

		void incrForPrim(final IRNode field) {
			if (JavaNode.getModifier(field, JavaNode.FINAL)) {					
				immutable += numDecls(field);
			} else {
				handleEachDecl(field, true);
			}
		}			
				
		private int numDecls(IRNode field) {
			IRNode decls = FieldDeclaration.getVars(field);
			return JJNode.tree.numChildren(decls);
		}
		
		private void handleEachDecl(final IRNode field, final boolean isPrim) {
			final IRNode decls = FieldDeclaration.getVars(field);
			for(final IRNode vd : VariableDeclarators.getVarIterator(decls)) {
				VouchFieldIsPromiseDrop vouch = LockRules.getVouchFieldIs(vd);
				if (vouch != null) {
					if (isPrim && vouch.isFinal() || vouch.isImmutable()) {
						immutable++;
						continue;
					}
					else if (vouch.isThreadSafe()) {
						threadSafe++;
						continue;
					}
				}
				IRegion region = RegionModel.getInstance(vd);
				if (lockUtils.getLockForRegion(clazz, region) != null) {
					locked++;
				}
				else if (LockRules.getThreadConfinedDrop(vd) != null) {
					threadConfined++;
				}			
				else {
					other++;
				}
			}
		}
		
		void recordAsDrop() {
			if (immutable + threadSafe + locked + threadConfined + other == 0) {
				return;
			}
			final MetricDrop d = new MetricDrop(clazz.getDeclaration(), IMetricDrop.Metric.STATE_WRT_CONCURRENCY);			
			if (immutable > 0) {
				addMetric(d, IMetricDrop.CONCURR_IMMUTABLE_COUNT, immutable);
			}
			if (threadSafe > 0) {
				addMetric(d, IMetricDrop.CONCURR_THREADSAFE_COUNT, threadSafe);
			}
			if (locked > 0) {
				addMetric(d, IMetricDrop.CONCURR_LOCK_PROTECTED_COUNT, locked);
			}
			if (threadConfined > 0) {
				addMetric(d, IMetricDrop.CONCURR_THREAD_CONFINED_COUNT, threadConfined);
			}
			if (other > 0) {
				addMetric(d, IMetricDrop.CONCURR_OTHER_COUNT, other);
			}
		}
				
		private void addMetric(MetricDrop d, String key, int value) {
			final IKeyValue info = KeyValueUtility.getIntInstance(key, value);
			d.addOrReplaceMetricInfo(info);
		}
	}
}
