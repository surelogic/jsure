package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.promise.AbstractLockDeclarationNode;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.analysis.concurrency.heldlocks.LockUtils;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.CommonStrings;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.DropPredicate;
import edu.cmu.cs.fluid.sea.IDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.util.Hashtable2;
import edu.cmu.cs.fluid.util.Pair;

/**
 * Promise drop for "lock" models.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 * 
 * @lock LockModelLock is class protects nameToDrop
 */
public final class LockModel extends ModelDrop<AbstractLockDeclarationNode> implements ILockBinding {
  /**
   * Map from lock names to drop instances (String -> RegionDrop).
   * <p>
   * Accesses must be protected by a lock on this class.
   */
  private static final Hashtable2<String, String, LockModel> NAME_TO_DROP = new Hashtable2<String, String, LockModel>();

  /*
   * This name-based lookup is very shaky. There should be a better way of
   * doing this.
   */

  /**
   * @param lockName
   *          The qualified name of the lock
   */
  public static LockModel getInstance(String lockName, String project) {
    synchronized (LockModel.class) {
      purgeUnusedLocks(); // cleanup the locks

      String key = lockName;
      LockModel result = NAME_TO_DROP.get(key, project);
      if (result == null) {
        key = CommonStrings.intern(lockName);
        result = new LockModel(key);

        NAME_TO_DROP.put(key, project, result);

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
  }

  public static LockModel getInstance(String lockName, IRNode context) {
    IIRProject p = JavaProjects.getEnclosingProject(context);
    final String project = p == null ? "" : p.getName();
    return getInstance(lockName, project);
  }

  /**
   * The simple lock name this drop represents the declaration for.
   */
  private final String f_lockName;

  /**
   * private constructor invoked by {@link #getInstance(String)}.
   * 
   * @param lockName
   *          the lock name
   */
  private LockModel(String lockName) {
    f_lockName = lockName;
    this.setMessage("lock " + lockName);
    this.setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }

  public String getQualifiedName() {
    return f_lockName;
  }

  public String getSimpleName() {
    AbstractLockDeclarationNode ld = getAAST();
    return ld.getId();
  }

  private static final DropPredicate definingDropPred = new DropPredicate() {
    public boolean match(IDrop d) {
      return d.instanceOf(RequiresLockPromiseDrop.class) || d.instanceOf(ReturnsLockPromiseDrop.class);
    }
  };

  /**
   * Removes locks that are no longer defined by any promise definitions.
   */
  public static void purgeUnusedLocks() {
    final Hashtable2<String, String, LockModel> newMap = new Hashtable2<String, String, LockModel>();
    synchronized (LockModel.class) {
      for (Pair<String, String> key : NAME_TO_DROP.keys()) {
        LockModel drop = NAME_TO_DROP.get(key.first(), key.second());

        boolean lockDefinedInCode = modelDefinedInCode(definingDropPred, drop);

        if (lockDefinedInCode) {
          newMap.put(key.first(), key.second(), drop);
        } else {
          // System.out.println("Purging lock "+key);
          drop.invalidate();
        }
        NAME_TO_DROP.clear();
        NAME_TO_DROP.putAll(newMap);
      }
    }
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