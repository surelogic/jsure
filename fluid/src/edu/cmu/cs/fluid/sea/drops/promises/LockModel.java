package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.*;

import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.promise.AbstractLockDeclarationNode;
import com.surelogic.analysis.concurrency.heldlocks.LockUtils;
import com.surelogic.common.i18n.I18N;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.ResultDrop;
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
   * Map from (lock name, project name) to drop instances.
   * <p>
   * Accesses must be protected by a lock on this class.
   */
  private static final Map<Pair<String, String>, LockModel> LOCKNAME_PROJECT_TO_DROP = new HashMap<Pair<String, String>, LockModel>();

  /*
   * This name-based lookup is very shaky. There should be a better way of doing
   * this.
   */

  private static LockModel getInstance(Pair<String, String> key) {
    synchronized (LockModel.class) {
      LockModel result = LOCKNAME_PROJECT_TO_DROP.get(key);
      return result;
    }
  }

  public static LockModel getInstance(String lockName, IRNode context) {
    return getInstance(getPair(lockName, context));
  }

  /**
   * The simple lock name this drop represents the declaration for.
   */
  private final String f_lockName;

  /**
   * @param lockName
   *          the lock name
   */
  private LockModel(AbstractLockDeclarationNode decl, String lockName) {
    super(decl);
    f_lockName = lockName;
    this.setResultMessage(15, lockName);
    this.setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }

  public static LockModel create(AbstractLockDeclarationNode decl, String lockName) {
    if (decl == null)
      throw new IllegalArgumentException(I18N.err(44, "decl"));
    if (lockName == null)
      throw new IllegalArgumentException(I18N.err(44, "lockName"));

    LockModel result = new LockModel(decl, lockName);
    synchronized (LockModel.class) {
      LOCKNAME_PROJECT_TO_DROP.put(getPair(lockName, decl.getPromisedFor()), result);
    }

    if ("java.lang.Object.MUTEX".equals(lockName)) {
      final String msg = "java.lang.Object.MUTEX is consistent with the code in java.lang.Object";
      ResultDrop rd = new ResultDrop(decl.getPromisedFor());
      rd.addCheckedPromise(result);
      rd.setConsistent();
      rd.setResultMessage(12, msg);
    }
    return result;
  }

  public String getQualifiedName() {
    return f_lockName;
  }

  public String getSimpleName() {
    AbstractLockDeclarationNode ld = getAAST();
    return ld.getId();
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