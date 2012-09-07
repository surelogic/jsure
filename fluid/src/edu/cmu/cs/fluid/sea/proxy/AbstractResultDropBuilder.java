package edu.cmu.cs.fluid.sea.proxy;

import java.util.HashSet;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.ResultFolderDrop;

/**
 * Abstract base builder to help analyses be parallelized for both
 * {@link ResultDrop} and {@link ResultFolderDrop} instances.
 */
public abstract class AbstractResultDropBuilder extends AbstractDropBuilder {
  protected Set<PromiseDrop<? extends IAASTRootNode>> checks = new HashSet<PromiseDrop<? extends IAASTRootNode>>();

  /**
   * Adds a promise to the set of promises this result establishes, or
   * <i>checks</i>.
   * 
   * @param promise
   *          the promise being supported by this result
   */
  public final void addCheckedPromise(PromiseDrop<? extends IAASTRootNode> promise) {
    if (promise == null) {
      throw new NullPointerException();
    }
    checks.add(promise);
  }

  /**
   * @return the set of promise drops established, or checked, by this result.
   *         All members of the returned set will are of the PromiseDrop type.
   */
  public Set<? extends PromiseDrop<? extends IAASTRootNode>> getChecks() {
    return checks;
  }
}
