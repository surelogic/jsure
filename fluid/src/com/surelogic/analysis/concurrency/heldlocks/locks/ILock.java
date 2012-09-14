package com.surelogic.analysis.concurrency.heldlocks.locks;

import com.surelogic.aast.promise.AbstractLockDeclarationNode;
import com.surelogic.dropsea.ir.drops.promises.LockModel;

public interface ILock {
  public enum Type { 
    MONOTLITHIC {
      @Override
      public String getPostFix() {
        return "";
      }
    },
    
    READ {
      @Override
      public String getPostFix() {
        return ".readLock()";
      }
    },
    
    WRITE {
      @Override
      public String getPostFix() {
        return ".writeLock()";
      }      
    };
    
    public static Type get(final boolean isWrite, final boolean isRW) {
      return isRW ? (isWrite ? WRITE : READ) : MONOTLITHIC;
    }
    
    public static Type getRW(final boolean isWrite) {
      return isWrite ? WRITE : READ;
    }
    
    public abstract String getPostFix();
  }
  
  /** 
   * Get the IRNode of the lock declaration for the lock.
   */
  public AbstractLockDeclarationNode getLockDecl();

  /** 
   * Convenience method for getting the name of the lock.
   */  
  public String getName();
  
  /**
   * @return Returns the lockPromise.
   */
  public LockModel getLockPromise();
  
  /**
   * Must the the lock allow writes?
   */
  public boolean isWrite();
}
