/*
 * Created on Nov 18, 2003
 *
 */
package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * @author chance
 *
 */
public abstract class AbstractPromiseParsedCallback implements IPromiseParsedCallback {
  /**
   * done() was called since last call to reset()
   */
  private boolean done;

  @Override
  public void reset() {
    done = false;
  }

  @Override
  public boolean wasCalled() {
    return done;
  }

  private void done() {
    if (done) {
      return;
    }
    finish();
    done = true;
  }

  /*
   * Only to be called by done(), and only once
   */
  protected abstract void finish();

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.promise.IPromiseParsedCallback#parsed()
	 */
	@Override
  public void parsed() {
    /*
    if (done) {
      noteWarning("Promise already declared to be parsed.");
    }
    */
    done();
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.promise.IPromiseParsedCallback#parsed(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
  public void parsed(IRNode n) {
		done();
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.eclipse.promise.IPromiseParsedCallback#notParsed(java.lang.String)
	 */
	@Override
  public void noteProblem(String description) {
    done();
	}
}
