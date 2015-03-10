/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/scrub/SimpleScrubber.java,v 1.5 2007/10/17 13:49:47 chance Exp $*/
package com.surelogic.annotation.scrub;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * Only includes the basics needed to implement a scrubber,
 * mostly to support creation of scrubbers that don't specifically
 * iterate over AASTs
 * 
 * @author Edwin.Chan
 */
public abstract class SimpleScrubber extends AbstractScrubber {  
  public SimpleScrubber(String name, String... deps) {
    super(NONE, name, ScrubberOrder.NORMAL, deps);
  }
  
  public SimpleScrubber(String name, ScrubberOrder order, String... deps) {
	super(NONE, name, order, deps);
  }

  @Override
  public final void run() {
    IDE.runAtMarker(new AbstractRunner() {
      @Override
      public void run() {
        scrub();
        AASTStore.sync();
      }
    });
  }
  
  protected abstract void scrub();
}
