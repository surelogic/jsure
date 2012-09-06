package com.surelogic.javac.coe;

import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public abstract class NameSorter {
  static final Logger LOG = SLLogger.getLogger("NameSorter");

  public void sort(Object[] elements) {
    Arrays.sort(elements, new Comparator<Object>() {
      public int compare(Object a, Object b) {
        return NameSorter.this.compare(a, b);
      }
    });
  }

  public abstract String getEnclosingFilePath(Object file);

  @SuppressWarnings("unchecked")
  public int compare(Object e1, Object e2) {
    int result; // = super.compare(viewer, e1, e2);
    boolean bothContent = (e1 instanceof Content) && (e2 instanceof Content);
    if (bothContent) {
      Content c1 = (Content) e1;
      Content c2 = (Content) e2;
      boolean c1IsNonProof = (c1.isInfo || c1.isPromiseWarning);
      boolean c2IsNonProof = (c2.isInfo || c2.isPromiseWarning);
      if (c1IsNonProof && !c2IsNonProof) {
        result = 1;
      } else if (c2IsNonProof && !c1IsNonProof) {
        result = -1;
      } else {
        boolean c1isPromise = c1.referencedDrop instanceof PromiseDrop;
        boolean c2isPromise = c2.referencedDrop instanceof PromiseDrop;
        if (c1isPromise && !c2isPromise) {
          result = 1;
        } else if (c2isPromise && !c1isPromise) {
          result = -1;
        } else {
          result = c1.getMessage().compareTo(c2.getMessage());
          if (result == 0) {
            final ISrcRef ref1 = c1.getSrcRef();
            final ISrcRef ref2 = c2.getSrcRef();
            if (ref1 != null && ref2 != null) {
              final Object file1 = ref1.getEnclosingFile();
              final Object file2 = ref2.getEnclosingFile();

              result = getEnclosingFilePath(file1).compareTo(getEnclosingFilePath(file2));

              if (result == 0) {
                final int line1 = ref1.getLineNumber();
                final int line2 = ref2.getLineNumber();
                result = (line1 == line2) ? 0 : ((line1 < line2) ? -1 : 1);
              }
            }
          }
        }
      }
    } else {
      LOG.warning("e1 and e2 are not Content objects: e1 = \"" + e1.toString() + "\"; e2 = \"" + e2.toString() + "\"");
      return -1;
    }

    return result;
  }
}