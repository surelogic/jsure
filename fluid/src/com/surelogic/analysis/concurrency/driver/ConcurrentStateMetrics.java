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
import com.surelogic.dropsea.ir.drops.type.constraints.*;

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

	enum PartStatus { NO_POLICY, NOT_THREADSAFE, THREADSAFE, IMMUTABLE }
	
	class FieldCounts {
		final IJavaDeclaredType clazz;
		final LockUtils lockUtils;
		final PartStatus instancePart, staticPart;
		
		int threadSafe = 0, immutable = 0, locked = 0, threadConfined = 0, other = 0;
		int notThreadSafe = 0, finalFields = 0;

		FieldCounts(IRNode t, LockUtils utils) {
			clazz = (IJavaDeclaredType) binder.getTypeEnvironment().convertNodeTypeToIJavaType(t);
			lockUtils = utils;
			
			// Figure out what annotations are on the type decl
			PartStatus iPart = PartStatus.NO_POLICY, sPart = PartStatus.NO_POLICY;
			final ImmutablePromiseDrop immutable = LockRules.getImmutableType(t);
			switch (immutable.getAppliesTo()) {
			case InstanceAndStatic:
				sPart = PartStatus.IMMUTABLE;				
			case Instance:
				iPart = PartStatus.IMMUTABLE;
				break;
			case Static:
				sPart = PartStatus.IMMUTABLE;
				break;
			}
			final ThreadSafePromiseDrop threadSafe = LockRules.getThreadSafeType(t);
			switch (threadSafe.getAppliesTo()) {
			case InstanceAndStatic:
				sPart = PartStatus.THREADSAFE;				
			case Instance:
				iPart = PartStatus.THREADSAFE;
				break;
			case Static:
				sPart = PartStatus.THREADSAFE;
				break;
			}
			NotThreadSafePromiseDrop notThreadSafe = LockRules.getNotThreadSafe(t);
			switch (notThreadSafe.getAppliesTo()) {
			case InstanceAndStatic:
				sPart = PartStatus.NOT_THREADSAFE;				
			case Instance:
				iPart = PartStatus.NOT_THREADSAFE;
				break;
			case Static:
				sPart = PartStatus.NOT_THREADSAFE;
				break;
			}
			instancePart = iPart;
			staticPart = sPart;
		}	
		
		void incrForArray(IRNode field) {
			handleEachDecl(field, false, PartStatus.NO_POLICY);
		}

		void incrForRef(final IRNode field, final IJavaSourceRefType st) {
			if (LockRules.isImmutableType(st.getDeclaration())) {
				handleEachDecl(field, false, PartStatus.IMMUTABLE);
			}
			else if (LockRules.isThreadSafeType(st.getDeclaration())) {
				handleEachDecl(field, false, PartStatus.THREADSAFE);
			}
			else handleEachDecl(field, false, PartStatus.NO_POLICY);
		} 

		void incrForPrim(final IRNode field) {				
			handleEachDecl(field, true, PartStatus.NO_POLICY);
		}			
		
		private void handleEachDecl(final IRNode field, final boolean isPrim, final PartStatus annoOnType) {
			final boolean isStatic = JavaNode.getModifier(field, JavaNode.STATIC);
			final PartStatus annoOnEnclosingType = isStatic ? staticPart : instancePart;
			
			final boolean isFinal = JavaNode.getModifier(field, JavaNode.FINAL);
			final IRNode decls = FieldDeclaration.getVars(field);
			for(final IRNode vd : VariableDeclarators.getVarIterator(decls)) {
				IRegion region = RegionModel.getInstance(vd);
				if (lockUtils.getLockForRegion(clazz, region) != null) {
					locked++;
					continue;
				}
				else if (LockRules.getThreadConfinedDrop(vd) != null) {
					threadConfined++;
					continue;
				}	
				
				PartStatus annoForField = annoOnType;
				boolean fieldIsFinal = isFinal;
				VouchFieldIsPromiseDrop vouch = LockRules.getVouchFieldIs(vd);
				if (vouch != null) {
					if (vouch.isFinal()) {
						fieldIsFinal = true;
					}
					else if (vouch.isImmutable()) {
						annoForField = PartStatus.IMMUTABLE;
					}
					else if (vouch.isThreadSafe() && annoOnType != PartStatus.IMMUTABLE) {
						annoForField = PartStatus.THREADSAFE;
					}
				}
				if (fieldIsFinal && (isPrim || annoForField == PartStatus.IMMUTABLE)) {
					finalFields++;
				} else {
					PartStatus anno = (annoOnEnclosingType.ordinal() > annoForField.ordinal()) ? annoOnEnclosingType : annoForField;
					switch (anno) {
					case IMMUTABLE:
						immutable++;
						break;
					case THREADSAFE:
						threadSafe++;
						break;
					case NOT_THREADSAFE:
						notThreadSafe++;
						break;
					case NO_POLICY:
					default:
						other++;
					}				
				}
			}
		}
		
		void recordAsDrop() {
			if (immutable + threadSafe + locked + threadConfined + other + notThreadSafe + finalFields== 0) {
				return;
			}
			final MetricDrop d = new MetricDrop(clazz.getDeclaration(), IMetricDrop.Metric.STATE_WRT_CONCURRENCY);			
			if (immutable > 0) {
				addMetric(d, IMetricDrop.CONCURR_IMMUTABLE_COUNT, immutable + finalFields);
			}
			if (threadSafe > 0) {
				addMetric(d, IMetricDrop.CONCURR_THREADSAFE_COUNT, threadSafe);
			}
			if (notThreadSafe > 0) {
				addMetric(d, IMetricDrop.CONCURR_NOTTHREADSAFE_COUNT, notThreadSafe);
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
