package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.IDiffInfo;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IResultDrop;

/**
 * Originally designed for use with regression tests, where the code doesn't
 * really change all that much
 * 
 * @author Edwin
 */
public class DefaultCategoryMatcher extends CategoryMatcher {	
  protected static final IDropMatcher matchDeclAndOffset = new AbstractDropMatcher("Decl   ", false) {
	  public boolean match(IDrop n, IDrop o) {
		  return matchBasics(n, o) && matchIDecls(n.getJavaRef(), o.getJavaRef()) &&
		         (matchIntDiffInfo(IDiffInfo.DECL_RELATIVE_OFFSET, n, o) || 
		    	  matchIntDiffInfo(IDiffInfo.DECL_END_RELATIVE_OFFSET, n, o));
	  }
  };
  
  protected static final IDropMatcher matchIdAndOffset = new AbstractDropMatcher("Id     ", false) {
	  public boolean match(IDrop n, IDrop o) {
		  return matchBasics(n, o) && 
		         getOffset(n) == getOffset(o) &&
		  	     matchStrings(getJavaId(n), getJavaId(o), false) == Boolean.TRUE;	         
	  }
  };
  
  protected static final IDropMatcher matchOffset = new AbstractDropMatcher("Offset ", false) {
	  public boolean match(IDrop n, IDrop o) {
		  return matchBasics(n, o) && 
	             getOffset(n) == getOffset(o);
	  }
  };
  
  protected static final IDropMatcher matchHashes = new AbstractDropMatcher("Hashed ", false) {
	  public boolean match(IDrop n, IDrop o) {
		  return matchBasics(n, o) && 
		         matchLongDiffInfo(IDiffInfo.FAST_TREE_HASH, n, o) &&
		         matchLongDiffInfo(IDiffInfo.FAST_CONTEXT_HASH, n, o);
	  }
  };

  static class AlsoMatchHints extends AbstractDropMatcher {
	  final IDropMatcher base;

	  protected AlsoMatchHints(String l, IDropMatcher base) {
		  super(l, base.warnIfMatched());
		  this.base = base;
	  }

	  public final boolean match(IDrop n, IDrop o) {
		  return base.match(n, o) && matchSupportingInfo(n, o);
	  }
  }
  
  protected DefaultCategoryMatcher() {
	  for(IDropMatcher m : passes) {
		  addPass(new AlsoMatchHints(m.getLabel().replace(' ', '+'), m));
		  addPass(m);
	  }
  }
  
  private static final IDropMatcher[] passes = {
	  matchDeclAndOffset, matchIdAndOffset, matchOffset, matchHashes
  };

  private static boolean matchResults(IDrop n, IDrop o) {
	return n instanceof IResultDrop &&
	       matchLongDiffInfo(IDiffInfo.FAST_TREE_HASH, n, o);
  }
}
