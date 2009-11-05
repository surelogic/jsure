package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.*;

import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.promise.AbstractLockDeclarationNode;
import com.surelogic.analysis.locks.LockUtils;

import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.*;

/**
 * Promise drop for "lock" models.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 * 
 * @lock LockModelLock is class protects nameToDrop
 */
public final class LockModel extends ModelDrop<AbstractLockDeclarationNode>
		implements ILockBinding {
	/**
	 * Map from lock names to drop instances (String -> RegionDrop).
	 */
	private static Map<String, LockModel> nameToDrop = new HashMap<String, LockModel>();

	/*
	 * This name-based lookup is very shakey. There should be a better way of
	 * doing this.
	 */
	/**
	 * @param lockName
	 *            The qualified name of the lock
	 */
	public static synchronized LockModel getInstance(String lockName) {
		purgeUnusedLocks(); // cleanup the locks

		String key = lockName;
		LockModel result = nameToDrop.get(key);
		if (result == null) {
			key = CommonStrings.intern(lockName);
			result = new LockModel(key);

			nameToDrop.put(key, result);

			if ("java.lang.Object.MUTEX".equals(key)) {
				result.setFromSrc(true); // Make it show up in the view
				final String msg = "java.lang.Object.MUTEX is consistent with the code in java.lang.Object";
				ResultDrop rd = new ResultDrop(msg);
				rd.addCheckedPromise(result);
				rd.setConsistent();
				rd.setMessage(msg);
			}
			// System.out.println("Creating lock "+key);
		}
		return result;
	}

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
		AbstractLockDeclarationNode ld = getAST();
		return ld.getId();
	}

	private static DropPredicate definingDropPred = new DropPredicate() {
		public boolean match(Drop d) {
			return d instanceof RequiresLockPromiseDrop
					|| d instanceof ReturnsLockPromiseDrop;
		}
	};

	/**
	 * Removes locks that are no longer defined by any promise definitions.
	 */
	public static synchronized void purgeUnusedLocks() {
		Map<String, LockModel> newMap = new HashMap<String, LockModel>();

		for (String key : nameToDrop.keySet()) {
			LockModel drop = nameToDrop.get(key);

			boolean lockDefinedInCode = modelDefinedInCode(definingDropPred,
					drop);

			if (lockDefinedInCode) {
				newMap.put(key, drop);
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
		return getAST().isReadWriteLock();
	}

	public boolean isJUCLock() {
		return getAST().isJUCLock();
	}

	public boolean isJUCLock(LockUtils u) {
		return getAST().isJUCLock(u);
	}

	public boolean isLockStatic() {
		return getAST().isLockStatic();
	}
}