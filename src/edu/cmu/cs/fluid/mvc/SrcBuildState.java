/*
 * Created on Jul 17, 2003
 *
 */
package edu.cmu.cs.fluid.mvc;

import com.surelogic.RequiresLock;

/**
 * Instances of this class keep track of the rebuild state of source
 * models.  To keep things simple for subclasses of
 * {@link AbstractModelToModelStatefulView}, instances are never explicitly 
 * informed of the complete set of possible source models.  Instead they
 * are learned dynamically as their state changes.  This should not be too
 * awful for performance because the set of sources ought to be learned
 * fairly quickly.  Actually, what is really important is the cardinality of
 * the set of source models.
 */
public final class SrcBuildState {
  /**
   * The set of source models that are currently rebuilding.  This
   * array grows dynamically as needed.  The cardinality of the set
   * is indicated by {@link #numBuilding}.  Elements of the set are
   * always stored in indices 0&ndash;<code>numBuilding-1</code>.
   * Any extra slots of the array contain <code>null</code>.
   *
   * <p>The state of the array is aggregated into the object.  The field
   * and the array contents must be accessed from <code>synchronized</code>
   * methods.
   */
  //@ unique buildling { Instance in Instance }
  private Model[] building;

  /**
   * The cardinality of the set stored in {@link #building}.  When
   * this field becomes zero then all the source models are quiescent.  The
   * field must be accessed from <code>synchronized</code> methods.
   */
  private int numBuilding;

  //@ lock Lock is this protects Instance

  /**
   * Create a new manager for source model rebuild state.
   */
  public SrcBuildState() {
    /* A view has at least one source, so don't waste time with a
     * zero-length array.
     */
    numBuilding = 0;
    building = new Model[1];
    building[0] = null;
  }

  /**
   * Wait for all the source models to be quiescent.
   * @exception InterruptedException Thrown if the thread is interrupted
   * while waiting.
   */
  public synchronized void waitForQuiescence() throws InterruptedException {
    while (numBuilding > 0) {
      this.wait();
    }
  }

  /**
   * Query if the source models are quiescent.
   * @return <code>true</code> iff the source models are believed to be 
   * quiescent.
   */
  public synchronized boolean isQuiescent() {
    return (numBuilding == 0);
  }

  /**
   * Toggle the rebuild state of a particular source model.
   * @param src The model whose state has changed.
   * @param isRebuild <code>true</code> if <code>src</code> is now
   * rebuilding, <code>false</code> if it is completed rebuilding.
   */
  public synchronized void toggleRebuildState(
    final Model src,
    final boolean isRebuild) {
    if (isRebuild == RebuildEvent.REBUILD_BEGUN) {
      insertModel(src);
    } else {
      removeModel(src);
    }
  }

  /**
   * Add a model to the set of rebuilding models.  Must be called from
   * a synchronized method.
   * @param src The model to add to the set.
   */
  //@ requires lock Lock
  private void insertModel(final Model src) {
    // see if the underlying array must grow
    if (numBuilding == building.length) {
      final Model[] newSet = new Model[numBuilding + 1];
      System.arraycopy(building, 0, newSet, 0, numBuilding);
      building = newSet;
    }

    // insert the new element
    building[numBuilding] = src;
    numBuilding += 1;
  }

  /**
   * Remove a model from the set of rebuilding models.  Must be called from
   * a synchronized method.
   * @param src The model to remove from the set.
   */
  //@ requires lock Lock
  @RequiresLock("MUTEX")
  private void removeModel(final Model src) {  	
    // find the location of src in the array
    int where = 0;
    while (where < building.length && building[where] != src) {
      where += 1;      
    }
    if (where >= numBuilding) {
    	return;
    }
		
    // compact the array
    try {
      System.arraycopy(
        building,
        where + 1,
        building,
        where,
        numBuilding - where - 1);
    } catch (ArrayIndexOutOfBoundsException ex) {
      ex.printStackTrace();
    }
    // decrement the cardinality
    numBuilding -= 1;
    if (numBuilding == 0) {
      this.notifyAll();
    }
  }
}