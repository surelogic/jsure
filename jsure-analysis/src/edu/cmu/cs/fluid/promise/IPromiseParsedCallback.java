package edu.cmu.cs.fluid.promise;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * @author chance
 */
public interface IPromiseParsedCallback {

  /**
   * Logger for this class
   */
  static final Logger LOG = SLLogger.getLogger("ECLIPSE.fluid.promise");

  /**
   * This should be called to reset the 'called' status
   * before starting to parse a promise
   *
   */
  void reset();

  /**
   * Returns true if any of the methods below, e.g. parsed() or note*(), were called.
   */
  boolean wasCalled();

  /**
   * This should only be called once per promise 
   * (i.e. once after each reset)
   */
  void parsed();

  /**
   * Could be called multiple times if there are multiple IRNode results
   * @param n
   */
  void parsed(IRNode n);

  /**
   * Could be called multiple times if there are problems 
   * @param description
   */
  void noteProblem(String description);

  /**
   * Called if there are other problems 
   * @param description
   */
  void noteWarning(String description);

  IPromiseParsedCallback defaultPrototype = new AbstractPromiseParsedCallback() {

    @Override
    public void noteProblem(String desc) {
      super.noteProblem(desc);
      LOG.warning("Ignoring notParsed: " + desc);
    }

    @Override
    public void noteWarning(String desc) {
      LOG.warning("Ignoring warning: " + desc);
    }

    @Override
    protected void finish() {
    }
  };
}
