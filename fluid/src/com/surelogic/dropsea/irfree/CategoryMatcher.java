package com.surelogic.dropsea.irfree;

import java.util.*;

import com.surelogic.common.SLUtility;
import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IDiffInfo;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.irfree.drops.IRFreeDrop;
import com.surelogic.persistence.JavaIdentifier;

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
    if (DiffCategory.suppressFilteredDrops && result == Boolean.FALSE) {
    	if (n.getMessage().startsWith(newPrefix)) {
    		String reconstructed = oldPrefix + n.getMessage().substring(newPrefix.length());
    		result = matchStrings(reconstructed, o.getMessage(), true);
    	}
    }
    return result != null ? result : false;
  }

  private static final String oldPrefix = "Borrowed(\"arg0\") on ";
  private static final String newPrefix = "Borrowed on parameter 'arg0' of ";
  
  protected static boolean matchAnalysisHint(IDrop n, IDrop o) {
	return matchStrings(n.getDiffInfoOrNull(IDiffInfo.ANALYSIS_DIFF_HINT), 
			            o.getDiffInfoOrNull(IDiffInfo.ANALYSIS_DIFF_HINT), false) == Boolean.TRUE;  
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

  // Need until we update all the oracles that use JavaIds
  protected static String getJavaId(IDrop d) {
    IJavaRef ref = d.getJavaRef();
    if (ref != null) {
      String id;
      if (ref instanceof IRFreeDrop) {
        id = ((IRFreeDrop) ref).javaId;
        if (id != null) {
          return id;
        }
      }
      id = JavaIdentifier.encodeDecl(ref.getEclipseProjectNameOrEmpty(), ref);
      return id;
    }
    return null;
  }

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
	  final int nval = n.getDiffInfoAsInt(key, IDiffInfo.UNKNOWN);
	  final int oval = o.getDiffInfoAsInt(key, IDiffInfo.UNKNOWN);
	  if (nval == IDiffInfo.UNKNOWN || oval == IDiffInfo.UNKNOWN) {
		  return false;
	  }
	  return nval == oval;
  }
  
  protected static boolean matchLongDiffInfo(String key, IDrop n, IDrop o) {
	  final long nval = n.getDiffInfoAsLong(key, IDiffInfo.UNKNOWN);
	  final long oval = o.getDiffInfoAsLong(key, IDiffInfo.UNKNOWN);
	  if (nval == IDiffInfo.UNKNOWN || oval == IDiffInfo.UNKNOWN) {
		  return false;
	  }
	  return nval == oval;
  }
}
