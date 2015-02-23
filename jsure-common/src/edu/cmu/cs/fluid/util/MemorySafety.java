/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/util/MemorySafety.java,v 1.5 2008/06/24 19:13:12 thallora Exp $
 */
package edu.cmu.cs.fluid.util;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;


/**
 * A class that keeps a safety set of memory around in case memory
 * runs out.  It gives the opporunity to the client to perform some clean
 * up code that (one hopes) will release sufficient storage to permit 
 * continued functioning.  In the IR, we use it to unload/destroy some
 * IR in order to free up space.
 * @author boyland
 */
public abstract class MemorySafety {
  Logger LOG = SLLogger.getLogger("FLUID");
  int[] hedge1, hedge2;
  
  /**
   * Create a memory hedge of the given size.  This is the amount that will be
   * made available during cleanup operations.
   * @param bytes number of bytes to use for the hedge
   */
  public MemorySafety(int bytes) {
    hedge1 = new int[bytes/4];
  }
  
  public int useless(int i) {
    return hedge1[i]++;
  }
  
  /**
   * Check that at least the specified amount of memory remains.
   * If not, perform a recovery now.
   * @param bytes number of bytes needed
   * @throws OutOfMemory Error if even after recovery, this isn't possible
   */
  public void checkFree(int bytes) {
    if (Runtime.getRuntime().freeMemory() < bytes) {
      OutOfMemoryError e = new OutOfMemoryError("not enough free");
      handle(e);
    }
  }
  
  /**
   * Attempt to handle an out of memory error.
   * The hedge will be released and we will attempt to free memory using the
   * recovery function.  The recovery function must free at least 1/4 of the
   * hedge size, or will be judged a failure.
   * @param e error to handle.  It will be rethrown if recovery doesn't work
   */
  public void handle(OutOfMemoryError e) {
    if (hedge1 == null) throw e;
    int size = hedge1.length;
    hedge1 = null;
    System.gc(); // give back the hedge
    LOG.info("Recovering from OutOfMemory condition using " + size*4 + " bytes.");
    try {
      recover();
    } catch (OutOfMemoryError e2) {
      LOG.severe("Hedge was insufficient for recovery\n");
      throw e;
    }
    try {
      hedge1 = new int[size];
    } catch (OutOfMemoryError e3) {
      LOG.severe("Recovery consumed memory");
      throw e;
    }
    try {
      hedge2 = new int[size/4];
    } catch (OutOfMemoryError e4) {
      LOG.severe("Recovery did not free " + size + " bytes of memory");
      throw e;
    }
    hedge2 = null;
  }
  
  /**
   * Release memory so that operations can continue.
   * This must free at least about 1/4 the hedge size or
   * it will be deemed a failure.  The amount of the hedge will be 
   * available during execution.  This must be sufficient, and must
   * be returned before the recovery is done.
   */
  protected abstract void recover();

  /**
   * Release the memory hedge.  The hedge will not be used again.
   */
  public void release() {
    hedge1 = null;
  }
}
