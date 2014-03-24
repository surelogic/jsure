package com.surelogic.dropsea.irfree;

import com.surelogic.dropsea.*;

/**
 * Originally designed for use with regression tests, where the code doesn't
 * really change all that much
 * 
 * @author Edwin
 */
public class DefaultCategoryMatcher extends CategoryMatcher {	
  protected static final IDropMatcher matchDeclAndOffset = new AbstractDropMatcher("Decl   ", false, true) {
	  @Override
    public boolean match(IDrop n, IDrop o) {
		  if (matchBasics(n, o) && matchIDecls(n.getJavaRef(), o.getJavaRef())) {
			  if (n instanceof IProposedPromiseDrop) {
				  // Match assumption refs if proposals
				  IProposedPromiseDrop np = (IProposedPromiseDrop) n;
				  IProposedPromiseDrop op = (IProposedPromiseDrop) o;
				  if (!matchIDecls(np.getAssumptionRef(), op.getAssumptionRef())) {
					  return false;
				  }
			  }
			  return (matchIntDiffInfo(DiffHeuristics.DECL_RELATIVE_OFFSET, n, o) || 
					  matchIntDiffInfo(DiffHeuristics.DECL_END_RELATIVE_OFFSET, n, o));			  
		  }
		  return false;
	  }
	  
	  @Override
	  public int hash(IDrop d) {
		  int rv = hashBasics(d) + hashIDecl(d.getJavaRef());
		  if (d instanceof IProposedPromiseDrop) {
			  IProposedPromiseDrop p = (IProposedPromiseDrop) d;
			  rv += hashIDecl(p.getAssumptionRef());
		  }
		  return rv;
	  }
  };
  
  /*
  protected static final IDropMatcher matchIdAndOffset = new AbstractDropMatcher("Id     ", false) {
	  public boolean match(IDrop n, IDrop o) {
		  return matchBasics(n, o) && 
		         getOffset(n) == getOffset(o) &&
		  	     matchStrings(getJavaId(n), getJavaId(o), false) == Boolean.TRUE;	         
	  }
  };
  */
  
  protected static final IDropMatcher matchOffset = new AbstractDropMatcher("Offset ", false, true) {
	  @Override
    public boolean match(IDrop n, IDrop o) {
		  return matchBasics(n, o) && 
	             getOffset(n) == getOffset(o);
	  }
	  
	  @Override
	  public int hash(IDrop d) {
		  return hashBasics(d) + getOffset(d);
	  }
  };
  
  protected static final IDropMatcher matchHashes = new AbstractDropMatcher("Hashed ", false, false) {
	  @Override
    public boolean match(IDrop n, IDrop o) {
		  return matchBasics(n, o) && 
		         matchLongDiffInfo(DiffHeuristics.FAST_TREE_HASH, n, o) &&
		         matchLongDiffInfo(DiffHeuristics.FAST_CONTEXT_HASH, n, o);
	  }
  };

  // Written so they only match if both have the same hints
  static class AlsoMatchHints extends AbstractDropMatcher {
	  final IDropMatcher base;

	  protected AlsoMatchHints(String l, IDropMatcher base) {
		  super(l, base.warnIfMatched(), base.useHashing());
		  this.base = base;
	  }

	  @Override
    public final boolean match(IDrop n, IDrop o) {
		  return matchAnalysisHint(getLabel(), n, o) && base.match(n, o); // || matchSupportingInfo(n, o));
	  }
	  
	  @Override
	  public int hash(IDrop d) {
		  if (!hasAnalysisHint(d)) {
			  throw cantMatch;
		  }
		  return base.hash(d);// + hashAnalysisHint(d);
	  }
	  
	  static final IllegalArgumentException cantMatch = new IllegalArgumentException();
  }
  
  protected DefaultCategoryMatcher() {
	  for(final IDropMatcher m : passes) {
		  addPass(new AlsoMatchHints(m.getLabel().replace(' ', '+'), m));
		  addPass(m);
		  /*
		  addPass(new AbstractDropMatcher(m.getLabel(), m.warnIfMatched()) {
			@Override
			public boolean match(IDrop n, IDrop o) {
				boolean rv = m.match(n, o);
				if (matchAnalysisHintOrNull(m.getLabel(), n, o) == Boolean.FALSE) {
					System.out.println("Not matching");
				}
				return rv;
			}			  
		  });
          */
	  }
  }
  
  private static final IDropMatcher[] passes = {
	  matchDeclAndOffset, /*matchIdAndOffset,*/ matchOffset, matchHashes
  };

  /*
  private static boolean matchResults(IDrop n, IDrop o) {
	return n instanceof IResultDrop &&
	       matchLongDiffInfo(DiffHeuristics.FAST_TREE_HASH, n, o);
  }
  */
}
