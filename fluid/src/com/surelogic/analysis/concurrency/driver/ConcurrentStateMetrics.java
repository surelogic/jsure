package com.surelogic.analysis.concurrency.driver;

import com.surelogic.analysis.concurrency.heldlocks.LockUtils;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.*;
import com.surelogic.dropsea.ir.MetricDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.*;
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
		final MetricDrop d = new MetricDrop(typeDecl, IMetricDrop.Metric.STATE_WRT_CONCURRENCY);
		d.setMessage(25, JavaNames.getFullTypeName(typeDecl));
		
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
		counts.record(d);
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
			handleEachDecl(field);
		}

		void incrForRef(final IRNode field, final IJavaSourceRefType st) {
			if (LockRules.isImmutableType(st.getDeclaration())) {
				immutable += numDecls(field);
			}
			else if (LockRules.isThreadSafeType(st.getDeclaration())) {
				threadSafe += numDecls(field);
			}
			else handleEachDecl(field);
		}

		void incrForPrim(final IRNode field) {
			if (JavaNode.getModifier(field, JavaNode.FINAL)) {					
				immutable += numDecls(field);
			} else {
				handleEachDecl(field);
			}
		}			
				
		private int numDecls(IRNode field) {
			IRNode decls = FieldDeclaration.getVars(field);
			return JJNode.tree.numChildren(decls);
		}
		
		private void handleEachDecl(final IRNode field) {
			final IRNode decls = FieldDeclaration.getVars(field);
			for(final IRNode vd : VariableDeclarators.getVarIterator(decls)) {
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
		
		void record(MetricDrop d) {
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