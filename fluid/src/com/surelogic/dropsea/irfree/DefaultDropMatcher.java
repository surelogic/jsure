package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IResultDrop;

/**
 * Originally designed for use with regression tests, 
 * where the code doesn't really change all that much
 * 
 * @author Edwin
 */
public class DefaultDropMatcher extends DropMatcher {
	protected DefaultDropMatcher() {
		super("Exact  ", "Core   ", "Hashed ", "Hashed2", "Results");
	}
	
    @Override
    protected boolean warnIfMatched(int pass) {
      return pass >= 5;
    }

    @Override
    protected boolean match(int pass, IDrop n, IDrop o) {
      switch (pass) {
      case 0:
        return matchId(n, o);
      case 1:
        return matchExact(n, o);
      case 2:
        return matchCore(n, o);
      case 3:
        return matchHashedAndHints(n, o);
      case 4:
        return matchHashed(n, o);
      case 5:
        return matchResults(n, o);
      default:
        return false;
      }
    }

    private boolean matchId(IDrop n, IDrop o) {
      return matchBasics(n, o) && matchStrings(getJavaId(n), getJavaId(o), false) == Boolean.TRUE;
    }

    private boolean matchExact(IDrop n, IDrop o) {
      return matchCore(n, o) && matchSupportingInfo(n, o);
    }

    private boolean matchCore(IDrop n, IDrop o) {
      return matchBasics(n, o) && getOffset(n) == getOffset(o);
    }

    private boolean matchHashedAndHints(IDrop n, IDrop o) {
      return matchHashed(n, o) && matchSupportingInfo(n, o);
    }

    private boolean matchHashed(IDrop n, IDrop o) {
      return matchBasics(n, o) && n.getTreeHash() == o.getTreeHash() && n.getContextHash() == o.getContextHash();
    }

    private boolean matchResults(IDrop n, IDrop o) {
      return (n instanceof IResultDrop) && n.getTreeHash() == o.getTreeHash();
    }
  }
