package edu.cmu.cs.fluid.sea.proxy;

import java.util.HashSet;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.IIRAnalysis;

import edu.cmu.cs.fluid.sea.AbstractResultDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultFolderDrop;

/**
 * Temporary builder for {@link ResultFolderDrop} instances to help analyses be
 * parallelized.
 * 
 * TODO This isn't going to work. The problem is that it wants actual
 * {@link AbstractResultDrop} instances directly added to this class when we
 * should really be adding {@link AbstractResultDropBuilder} instances as
 * elements.
 * 
 * This works because promise drops are "static" before the parallel analysis is
 * run. The results, however, are not. We need a way to handle this that plays
 * nicely with the invoking of these builders. Not sure how to do this.
 * 
 * We also need example code on how to create results in a folder.
 */
public final class ResultFolderDropBuilder extends AbstractResultDropBuilder {
  private Set<AbstractResultDrop> contents = new HashSet<AbstractResultDrop>();

  private ResultFolderDropBuilder() {
  }

  public static ResultFolderDropBuilder create(IIRAnalysis a) {
    final ResultFolderDropBuilder rv = new ResultFolderDropBuilder();
    a.handleBuilder(rv);
    return rv;
  }

  public void add(AbstractResultDrop result) {
    contents.add(result);
  }

  @Override
  public int build() {
    if (!isValid()) {
      return 0;
    }
    ResultFolderDrop f = new ResultFolderDrop();
    for (PromiseDrop<? extends IAASTRootNode> check : checks) {
      f.addCheckedPromise(check);
    }
    for (AbstractResultDrop d : contents) {
      f.add(d);
    }
    return buildDrop(f);
  }
}
