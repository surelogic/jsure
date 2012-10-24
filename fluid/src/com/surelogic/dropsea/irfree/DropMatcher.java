package com.surelogic.dropsea.irfree;

import com.surelogic.common.ref.IDecl;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IDiffInfo;
import com.surelogic.dropsea.IHintDrop;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.irfree.drops.IRFreeDrop;
import com.surelogic.persistence.JavaIdentifier;

public abstract class DropMatcher {
  private final String[] labels;

  protected DropMatcher(String... labels) {
    if (labels == null) {
      throw new IllegalArgumentException("Null labels");
    }
    for (String l : labels) {
      if (l == null) {
        throw new IllegalArgumentException("Null label");
      }
      if (l.length() != 7) {
        throw new IllegalArgumentException("Incorrect length: " + l);
      }
    }
    this.labels = labels;
  }

  final int numPasses() {
    return labels.length;
  }

  /**
   * @return A String of length 7
   */
  final String getLabel(int pass) {
    return labels[pass];
  }

  protected abstract boolean warnIfMatched(int pass);

  protected abstract boolean match(int pass, IDrop n, IDrop o);

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
    return result != null ? result : false;
  }

  protected static boolean matchSupportingInfo(IDrop n, IDrop o) {
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
    if (n == null || o == null) {
    	return false;
    }
    return n.isSameDeclarationAsSloppy(o);
  }
  
  protected static boolean matchIntDiffInfo(String key, IDrop n, IDrop o) {
	  final int nval = n.getDiffInfoAsInt(key, IDiffInfo.UNKNOWN);
	  final int oval = o.getDiffInfoAsInt(key, IDiffInfo.UNKNOWN);
	  if (nval == IDiffInfo.UNKNOWN || oval == IDiffInfo.UNKNOWN) {
		  return false;
	  }
	  return nval == oval;
  }
}
