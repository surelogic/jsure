package edu.cmu.cs.fluid.control;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.util.Hashtable2;

public class LabelList {
  /** Logger instance for debugging. */
  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis");

  private LabelList() {
	  // Do nothing
  }
  
  public static final LabelList empty = new LabelList();
  private static final Hashtable2<LabelList,ControlLabel,LabelList> longer = 
    new Hashtable2<LabelList,ControlLabel,LabelList>();

  ControlLabel label = null;
  LabelList shorter = null;

  public LabelList addLabel(ControlLabel l) {
	LabelList ll;
	synchronized (LabelList.class) {
		ll = longer.get(this,l);
		if (ll == null) {
			ll = new LabelList();
			ll.label = l;
			ll.shorter = this;
			longer.put(this,l, ll);
		}
	}
    return ll;
  }

  public LabelList dropLabel(ControlLabel l) {
    if (label.equals(l))
      return shorter;
    else
      return null;
  }

  public LabelList dropLabel() {
    if (shorter == null && this != empty) {
      LOG.severe("dropping label yields null on non-empty list.");
    }
    return shorter;
  }

  public ControlLabel firstLabel() {
    return label;
  }

  /**
	 * Return whether this is an extension of another label list.
	 */
  public boolean includes(LabelList other) {
    if (this == other)
      return true;
    if (shorter == null)
      return false;
    return shorter.includes(other);
  }
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("<");
    LabelList ll = this;
    while (ll != empty) {
      if (ll != this)
        sb.append(",");
      sb.append(ll.label);
      ll = ll.dropLabel();
    }
    sb.append(">");
    return sb.toString();
  }
}
