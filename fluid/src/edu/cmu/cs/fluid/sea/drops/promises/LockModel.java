package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.*;

import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.promise.AbstractLockDeclarationNode;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.analysis.concurrency.heldlocks.LockUtils;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.util.*;

/**
 * Promise drop for "lock" models.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 * 
 * @lock LockModelLock is class protects nameToDrop
 */
public final class LockModel extends ModelDrop<AbstractLockDeclarationNode>
		implements ILockBinding {
	/**
	 * Map from lock names to drop instances (String -> RegionDrop).
	 */
	private static Hashtable2<String,String,LockModel> nameToDrop = new Hashtable2<String,String,LockModel>();

	/*
	 * This name-based lookup is very shakey. There should be a better way of
	 * doing this.
	 */
	/**
	 * @param lockName
	 *            The qualified name of the lock
	 */
	public static synchronized LockModel getInstance(String lockName, String project) {
		purgeUnusedLocks(); // cleanup the locks

		String key = lockName;
		LockModel result = nameToDrop.get(key, project);
		if (result == null) {
			key = CommonStrings.intern(lockName);
			result = new LockModel(key);

			nameToDrop.put(key, project, result);

			if ("java.lang.Object.MUTEX".equals(key)) {
				result.setFromSrc(true); // Make it show up in the view
				final String msg = "java.lang.Object.MUTEX is consistent with the code in java.lang.Object";
				ResultDrop rd = new ResultDrop();
				rd.addCheckedPromise(result);
				rd.setConsistent();
				rd.setMessage(msg);
			}
			// System.out.println("Creating lock "+key);
		}
		return result;
	}

	public static LockModel getInstance(String lockName, IRNode context) {
		IIRProject p = JavaProjects.getEnclosingProject(context);
		final String project = p == null ? "" : p.getName();
		return getInstance(lockName, project);
	}
	
	/*
	@Override
	protected final void invalidate_internal() {
		System.out.println("Invalidating "+getMessage());
	}
	*/
	/**
	 * The simple lock name this drop represents the declaration for.
	 */
	private final String lockName;

	/**
	 * private constructor invoked by {@link #getInstance(String)}.
	 * 
	 * @param name
	 *            the lock name
	 */
	private LockModel(String name) {
		lockName = name;
		this.setMessage("lock " + name);
		this.setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
	}

	public String getQualifiedName() {
		return lockName;
	}

	public String getSimpleName() {
		AbstractLockDeclarationNode ld = getAAST();
		return ld.getId();
	}

	private static DropPredicate definingDropPred = new DropPredicate() {
		public boolean match(IDrop d) {
			return d.instanceOf(RequiresLockPromiseDrop.class)
					|| d.instanceOf(ReturnsLockPromiseDrop.class);
		}
	};

	/**
	 * Removes locks that are no longer defined by any promise definitions.
	 */
	public static synchronized void purgeUnusedLocks() {
		Hashtable2<String,String,LockModel> newMap = new Hashtable2<String,String,LockModel>();

		
		for (Pair<String,String> key : nameToDrop.keys()) {
			LockModel drop = nameToDrop.get(key.first(), key.second());

			boolean lockDefinedInCode = 
				modelDefinedInCode(definingDropPred, drop);// && 
			    //drop.hasMatchingDependents(DropPredicateFactory.matchExactType(RegionModel.class));			

			if (lockDefinedInCode) {
				newMap.put(key.first(), key.second(), drop);
			} else {
				// System.out.println("Purging lock "+key);
				drop.invalidate();
			}
		}
		// swap out the static map to locks
		nameToDrop = newMap;
	}

	public LockModel getModel() {
		return this;
	}

	public boolean isReadWriteLock() {
		return getAAST().isReadWriteLock();
	}

	public boolean isJUCLock() {
		return getAAST().isJUCLock();
	}

	public boolean isJUCLock(LockUtils u) {
		return getAAST().isJUCLock(u);
	}

	public boolean isLockStatic() {
		return getAAST().isLockStatic();
	}
}