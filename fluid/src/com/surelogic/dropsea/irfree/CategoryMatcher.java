package com.surelogic.dropsea.irfree;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.common.SLUtility;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IHintDrop;

public abstract class CategoryMatcher {
  private final List<IDropMatcher> passes = new ArrayList<IDropMatcher>();

  final int numPasses() {
    return passes.size();
  }

  final IDropMatcher getPass(int pass) {
	return passes.get(pass);
  }
  
  protected final void addPass(IDropMatcher m) {
	final String l = m.getLabel();
	if (l == null) {
		throw new IllegalArgumentException("Null label");
	}
	if (l.length() != 7) {
		throw new IllegalArgumentException("Incorrect length: " + l);
	}
	passes.add(m);  
  }

  protected static boolean matchBasics(IDrop n, IDrop o) {
    return matchMessage(n, o);
  }

  protected static boolean matchCategory(IDrop n, IDrop o) {
    String nCat = n.getCategorizingMessage();
    String oCat = o.getCategorizingMessage();
    if (nCat != null && oCat != null) {
      return nCat.equals(oCat);
    }
    // If either null, don't compare
    return true;
  }

  private static String preprocess(String value) {
    if (value == null) {
      return null;
    }
    return value.trim();
  }

  protected static Boolean matchStrings(String n, String o, boolean startsWith) {
    String nMsg = preprocess(n);
    String oMsg = preprocess(o);
    if (nMsg != null && oMsg != null) {
      boolean result = nMsg.equals(oMsg);
      return result;
    }
    return null;
  }

  protected static boolean matchMessage(IDrop n, IDrop o) {
    Boolean result;
    // if (o.getMessageCanonical() != null &&
    // !o.getMessageCanonical().endsWith(" (EMPTY)")) { // TODO only needed for
    // summaries
    result = matchStrings(n.getMessageCanonical(), o.getMessageCanonical(), false);
    if (result != null && result.booleanValue()) {
      // Return if true
      // Otherwise, check the message
      return result;
    }
    // }    
    result = matchStrings(n.getMessage(), o.getMessage(), true);
    /*
    if (DiffCategory.suppressFilteredDrops && o.getMessage().startsWith("Borrowed")) {
    	for(int i=0; i<oldPrefix.length && result == Boolean.FALSE; i++) {
    		if (o.getMessage().startsWith(oldPrefix[i])) {
    			final String reconstructed = newPrefix[i] + o.getMessage().substring(oldPrefix[i].length());
    			result = matchStrings(n.getMessage(), reconstructed, true);
    		}
    	}    	
    }
    */
    return result != null ? result : false;
  }
  
  /*
  private static final String[] oldPrefix = {
	  "Borrowed(\"arg0\") on ",
	  "Borrowed(\"arg1\") on ",
	  "Borrowed(\"arg2\") on ",
  };
  private static final String[] newPrefix = {
	  "Borrowed on parameter 'arg0' of ",
	  "Borrowed on parameter 'arg1' of ",
	  "Borrowed on parameter 'arg2' of ",
  };
  */
  
  protected static boolean matchAnalysisHint(String label, IDrop n, IDrop o) {
	  return matchAnalysisHintOrNull(label, n, o) == Boolean.TRUE;
  }
  
  protected static Boolean matchAnalysisHintOrNull(String label, IDrop n, IDrop o) {
	final String nh = n.getDiffInfoOrNull(DiffHeuristics.ANALYSIS_DIFF_HINT);
	final String oh = o.getDiffInfoOrNull(DiffHeuristics.ANALYSIS_DIFF_HINT);
	/*
	if (nh != null && oh != null) {
		System.out.println("Got hints for "+n.getMessage()+": "+nh+", "+oh);
	}
	*/
	final Boolean rv = matchStrings(nh, oh, false);  
	/*
	if (rv == Boolean.TRUE) {
		System.out.println(label+" Matched hints: "+nh);
	}
	else if (rv != null) {
		System.out.println(label+" Not matched 1: "+nh);
		System.out.println(label+" Not matched 2: "+oh);
	}
    */
	return rv;
  }
  
  protected static boolean matchSupportingInfo(IDrop n, IDrop o) {
	if (!(n instanceof IAnalysisResultDrop) || 
		!(n instanceof IAnalysisResultDrop)) {
		return false;
	}
    final long oh = computeSIHash(o);
    if (DropDiff.allowMissingSupportingInfos && oh == 0) {
      return true;
    }
    final long nh = computeSIHash(n);
    if (nh != oh) {
      // System.out.println("Diff in infos");
      return false;
    }
    return DropDiff.isSame(n, o);
  }

  private static long computeSIHash(IDrop e) {
    long rv = 0;
    for (IHintDrop i : e.getHints()) {
      rv += i.getHintType().hashCode();

      String msg = i.getMessage();
      if (msg == null) {
        continue;
      }
      rv += msg.hashCode();
    }
    return rv;
  }

  protected static boolean isWithinX(final int maxDiff, int n, int o) {
	final int diff = Math.abs(n - o);
	return diff <= maxDiff;
  }
  
  protected static int getOffset(IDrop d) {
    IJavaRef ref = d.getJavaRef();
    if (ref != null) {
      return ref.getOffset();
    }
    return -1; // for unknown
  }

  /*
  // Need until we update all the oracles that use JavaIds
  protected static String getJavaId(IDrop d) {
    IJavaRef ref = d.getJavaRef();
    if (ref != null) {
      String id = JavaIdentifier.encodeDecl(ref.getEclipseProjectNameOrEmpty(), ref);
      return id;
    }
    return null;
  }
  */

  protected static boolean matchIDecls(IJavaRef nr, IJavaRef or) {
	if (nr == null || or == null) {
		return false;
	}
    if (nr.getPositionRelativeToDeclaration() != or.getPositionRelativeToDeclaration()) {
    	return false;
    }
    IDecl n = nr.getDeclaration();
    IDecl o = or.getDeclaration();
    boolean rv = n.isSameDeclarationAsSloppy(o);
    /*
    if (rv) {
    	n.isSameDeclarationAsSloppy(o);
    }
    */
    return rv;
  }
  
  protected static boolean matchIDeclsClosely(IJavaRef nr, IJavaRef or) {
	  if (nr == null || or == null) {
		  return false;
	  }
	  if (nr.getPositionRelativeToDeclaration() != or.getPositionRelativeToDeclaration()) {
		  return false;
	  }
	  final IDecl n = nr.getDeclaration();
	  final IDecl o = or.getDeclaration();
	  // Check if the parents are the same
	  if (n.getParent() != o.getParent()) {
		  if (n.getParent() == null || o.getParent() == null) {
			  return false;
		  }
		  if (!n.getParent().isSameDeclarationAsSloppy(o.getParent())) {
			  return false;
		  }
	  } // else both null or exactly the same

	  // Nearly the same location
	  if (!isWithinX(3, nr.getLineNumber(), or.getLineNumber())) {
		  return false;
	  }
	  
	  if (n.hasSameAttributesAsSloppy(o)) { // same name/kind
		  // Close parameters -- TODO how to check the parameter types?
		  return isWithinX(1, n.getParameters().size(), o.getParameters().size());
	  } 
	  else if (n.getKind() == IDecl.Kind.METHOD && SLUtility.nullSafeEquals(n.getKind(), o.getKind()) && 
			   n.isSameSimpleDeclarationAsSloppy(o, false)) { 
		  // same kind/parameters		  
		  return n.getTypeOf().getFullyQualified().equals(o.getTypeOf().getFullyQualified());
	  }
 	  return false;
  }
  
  protected static boolean matchIntDiffInfo(String key, IDrop n, IDrop o) {
	  final int nval = n.getDiffInfoAsInt(key, DiffHeuristics.UNKNOWN);
	  final int oval = o.getDiffInfoAsInt(key, DiffHeuristics.UNKNOWN);
	  if (nval == DiffHeuristics.UNKNOWN || oval == DiffHeuristics.UNKNOWN) {
		  return false;
	  }
	  return nval == oval;
  }
  
  protected static boolean matchLongDiffInfo(String key, IDrop n, IDrop o) {
	  final long nval = n.getDiffInfoAsLong(key, DiffHeuristics.UNKNOWN);
	  final long oval = o.getDiffInfoAsLong(key, DiffHeuristics.UNKNOWN);
	  if (nval == DiffHeuristics.UNKNOWN || oval == DiffHeuristics.UNKNOWN) {
		  return false;
	  }
	  return nval == oval;
  }
}
