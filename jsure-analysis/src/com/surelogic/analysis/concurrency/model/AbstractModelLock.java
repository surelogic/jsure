package com.surelogic.analysis.concurrency.model;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;

/**
 * Lock in the lock model.
 */
public abstract class AbstractModelLock<
    A extends PromiseDrop<? extends IAASTRootNode>,
    L extends LockImplementation>
implements ModelLock<A, L> {
  protected final A sourceDrop;
  protected final L lockImpl;
  // Derived from sourceDrop, so doesn't participate in hash code or equality
  protected final IJavaDeclaredType declaredInClass;
  
  protected AbstractModelLock(
      final A sourceDrop, final L lockImpl, final IRNode classNode) {
    this.sourceDrop = sourceDrop;
    this.lockImpl = lockImpl;
    this.declaredInClass = 
        (IJavaDeclaredType) JavaTypeFactory.getMyThisType(classNode);
  }

  
  
  @Override
  public final A getSourceAnnotation() {
    return sourceDrop;
  }
  
  @Override
  public final L getImplementation() {
    return lockImpl;
  }
  
  @Override
  public IJavaDeclaredType getDeclaredInClass() {
    return declaredInClass;
  }
  
  @Override
  public final boolean isStatic() {
    return lockImpl.isStatic();
  }
  
  @Override
  public final boolean isIntrinsic(final IBinder binder) {
    return lockImpl.isIntrinsic(binder);
  }
  
  @Override
  public final boolean isJUC(final IBinder binder) {
    return lockImpl.isJUC(binder);
  }

  @Override
  public boolean isReadWrite(final IBinder binder) {
    return lockImpl.isReadWrite(binder);
  }
  
  
  
  protected final int partialHashCode() {
    int result = 17;
    result += 31 * sourceDrop.hashCode();
    result += 31 * lockImpl.hashCode();
    return result;
  }
  
  protected final boolean partialEquals(final AbstractModelLock<?, ?> other) {
    return other.sourceDrop.equals(this.sourceDrop) &&
        other.lockImpl.equals(this.lockImpl);
  }
}
