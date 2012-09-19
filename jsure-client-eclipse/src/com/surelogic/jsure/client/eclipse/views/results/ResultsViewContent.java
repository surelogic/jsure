package com.surelogic.jsure.client.eclipse.views.results;

import static com.surelogic.common.jsure.xml.CoE_Constants.INFO;
import static com.surelogic.common.jsure.xml.CoE_Constants.INFO_WARNING;
import static com.surelogic.common.jsure.xml.CoE_Constants.REDDOT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.ir.Category;
import com.surelogic.tree.diff.Diff;
import com.surelogic.tree.diff.IDiffNode;

import edu.cmu.cs.fluid.java.ISrcRef;

final class ResultsViewContent implements Cloneable, IDiffNode<ResultsViewContent> {
  /**
   * Status for diffing
   */
  private Status f_status = Status.SAME;

  private int f_numIssues = -1;

  /**
   * The drop referenced, or {@code null}.
   */
  private final IDrop f_referencedDrop;

  /**
   * This items children for the viewer, meant to be accessed by
   * {@link #getChildren()}.
   * 
   * @see #getChildren()
   */
  private Collection<ResultsViewContent> f_children;

  /**
   * The message to display in the viewer, meant to be accessed by
   * {@link #getMessage()}.
   * 
   * @see #getMessage()
   */
  private final String f_message;

  /**
   * Cache of getMessage();
   */
  private String f_getMessage;

  private ResultsViewContent parent = null;

  private String f_baseImageName;

  private int f_imageFlags = 0;

  /**
   * The fAST node that this item references, or null, if the associated drop
   * defines the reference location.
   */
  private ISrcRef f_sourceRef = null;

  /**
   * <code>true</code> if this content is from an inference (or InfoDrop),
   * <code>false</code> otherwise.
   */
  boolean f_isInfo = false;

  boolean f_isInfoDecorated = false;

  /**
   * <code>true</code> if this content is from an inference (or InfoDrop) that
   * is a warning, <code>false</code> otherwise.
   */
  boolean f_isInfoWarning = false;

  boolean f_isInfoWarningDecorate = false;

  boolean f_donePropagatingWarningDecorators = false;

  /**
   * <code>true</code> if this content is from an promise warning drop (or
   * PromiseWarningDrop), <code>false</code> otherwise.
   */
  boolean f_isPromiseWarning = false;

  /**
   * A reference to the original node. Non-null only if it's a backedge
   */
  ResultsViewContent cloneOf = null;

  ResultsViewContent(String msg, Collection<ResultsViewContent> content, IDrop drop) {
    f_message = msg;
    f_children = content;
    for (ResultsViewContent c : content) {
      c.setParent(this);
    }
    f_referencedDrop = drop;
    if (drop != null)
      f_sourceRef = drop.getSrcRef();
  }

  ResultsViewContent(String msg, ISrcRef ref) {
    f_message = msg;
    f_children = Collections.emptyList();
    f_referencedDrop = null;
    f_sourceRef = ref;
  }

  ResultsViewContent cloneAsLeaf() {
    ResultsViewContent clone = shallowCopy();
    if (clone != null) {
      clone.f_status = Status.BACKEDGE;
      clone.cloneOf = this;
    }
    return clone;
  }

  public void setCount(int count) {
    f_numIssues = count;
  }

  public int freezeCount() {
    return f_numIssues = f_children.size();
  }

  public void freezeChildrenCount() {
    int size = 0;
    for (ResultsViewContent c : f_children) {
      size += c.freezeCount();
    }
    f_numIssues = size;
  }

  public int recomputeCounts() {
    if (f_numIssues < 0) {
      // No counts previously recorded here
      for (ResultsViewContent c : f_children) {
        c.recomputeCounts();
      }
      return -1;
    }
    boolean counted = false;
    int size = 0;
    for (ResultsViewContent c : f_children) {
      int count = c.recomputeCounts();
      if (count > 0) {
        size += count;
        counted = true;
      }
    }
    if (counted) {
      return f_numIssues = size;
    }
    return freezeCount();
  }

  public ISrcRef getSrcRef() {
    return f_sourceRef;
  }

  public String getMessage() {
    if (f_getMessage != null) {
      return f_getMessage;
    }
    String result = f_message;
    final ISrcRef ref = getSrcRef();
    if (ref != null) {
      String name = "?";

      Object f = ref.getEnclosingFile();
      if (f instanceof IResource) {
        IResource file = (IResource) f;
        name = file.getName();
      } else if (f instanceof String) {
        name = (String) f;
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
          name = name.substring(lastSlash + 1);
        }
      } else if (f != null) {
        name = f.toString();
      }

      final boolean referencesAResultDrop = getDropInfo() instanceof IResultDrop;
      if (ref.getLineNumber() > 0) {
        if (referencesAResultDrop) {
//          result += " at line " + ref.getLineNumber();
        } else {
//          result += " at " + name + " line " + ref.getLineNumber();
        }
      } else if (!name.equals("?")) {
        if (!referencesAResultDrop) {
//          result += " at " + name;
        }
      }
    } else if (f_numIssues > 0) {
      if (f_message.contains("s)")) {
        result = f_numIssues + " " + result;
      } else {
        result += " (" + f_numIssues + (f_numIssues > 1 ? " issues)" : " issue)");
      }
    }
    if (f_status != Status.SAME) {
      result += " -- " + f_status;
    }
    f_getMessage = result;
    return result;
  }

  public void setBaseImageName(String name) {
    if (name == null)
      throw new IllegalArgumentException("the base image can't be null");
    f_baseImageName = name;
  }

  public String getBaseImageName() {
    return f_baseImageName;
  }

  public void setImageFlags(int flags) {
    f_imageFlags = flags;
  }

  public int getImageFlags() {
    return f_imageFlags;
  }

  // For exporting the node (see XMLReport)
  public int getFlags() {
    int flagInt = getImageFlags();
    if (f_isInfoWarningDecorate) {
      flagInt |= INFO_WARNING;
    } else if (f_isInfoDecorated) {
      if (!CommonImages.IMG_INFO.equals(getBaseImageName()))
        flagInt |= INFO;
    }
    return flagInt;
  }

  // For exporting the node (see XMLReport)
  public boolean isRedDot() {
    if ((getImageFlags() & REDDOT) > 0) {
      return true;
    }
    return false;
  }

  public static Object[] filterNonInfo(Object[] items) {
    Set<ResultsViewContent> result = new HashSet<ResultsViewContent>();
    for (int i = 0; i < items.length; i++) {
      if (items[i] instanceof ResultsViewContent) {
        ResultsViewContent item = (ResultsViewContent) items[i];
        if (!item.f_isInfo) {
          result.add(item);
        }
      } else {
        SLLogger.getLogger().severe("FAILURE: filterNonInfo found non-Content item");
      }
    }
    return result.toArray();
  }

  public Object[] getNonInfoChildren() {
    return filterNonInfo(f_children.toArray());
  }

  public Object[] getChildren() {
    return f_children.toArray();
  }

  public Category getCategory() {
    if (getDropInfo() != null) {
      return getDropInfo().getCategory();
    }
    return null;
  }

  public void resetChildren(Collection<ResultsViewContent> c) {
    if (c == null) {
      throw new IllegalArgumentException(I18N.err(44, "c"));
    }
    f_children = c;
    for (ResultsViewContent cc : c) {
      cc.setParent(this);
    }
  }

  public void addChild(ResultsViewContent child) {
    if (f_children.size() == 0) {
      f_children = new ArrayList<ResultsViewContent>(1);
    }
    f_children.add(child);
    child.setParent(this);
  }

  public int numChildren() {
    return f_children.size();
  }

  public static Collection<ResultsViewContent> diffChildren(Collection<ResultsViewContent> last, Collection<ResultsViewContent> now) {
    Collection<ResultsViewContent> diffs = Diff.diff(last, now, false);
    for (ResultsViewContent c : diffs) {
      c.recomputeCounts();
    }
    return diffs;
  }

  public Collection<ResultsViewContent> getChildrenAsCollection() {
    return f_children;
  }

  public Collection<ResultsViewContent> setChildren(Collection<ResultsViewContent> c) {
    try {
      return f_children;
    } finally {
      f_children = c;
      for (ResultsViewContent cc : c) {
        cc.setParent(this);
      }
    }
  }

  public Status getStatus() {
    return f_status;
  }

  public Status setStatus(Status s) {
    try {
      return f_status;
    } finally {
      f_status = s;
    }
  }

  @Override
  public String toString() {
    if (f_sourceRef != null) {
      return f_message + " at " + f_sourceRef.getEnclosingFile() + ":" + f_sourceRef.getLineNumber();
    }
    return f_message;
  }

  public IDrop getDropInfo() {
    return f_referencedDrop;
  }

  public Object identity() {
    return id;
  }

  private class Identity {

    private ResultsViewContent content() {
      return ResultsViewContent.this;
    }

    @Override
    public int hashCode() {
      /*
       * if (referencedDrop != null) { return
       * referencedDrop.getMessage().hashCode(); }
       */
      if (f_message == null) {
        return 0;
      }
      return f_message.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Identity) {
        ResultsViewContent c = ((Identity) o).content();
        if (getDropInfo() == c.getDropInfo()) {
          return true;
        }
        if (f_message.equals(c.f_message)) {
          return f_sourceRef == c.f_sourceRef
              || (f_sourceRef != null && c.f_sourceRef != null && f_sourceRef.getEnclosingFile().equals(
                  c.f_sourceRef.getEnclosingFile()));
        }
      }
      return false;
    }
  }

  private final Identity id = new Identity();

  public final Comparator<Identity> getComparator() {
    return new Comparator<Identity>() {

      public int compare(Identity o1, Identity o2) {
        ResultsViewContent c1 = o1.content();
        ResultsViewContent c2 = o2.content();
        if (c1.getDropInfo() == c2.getDropInfo()) {
          return 0;
        }
        if (c1.f_message.equals(c2.f_message)) {
          if (c1.f_sourceRef == c2.f_sourceRef) {
            return 0;
          }
          if (c1.f_sourceRef != null && c2.f_sourceRef != null
              && c1.f_sourceRef.getEnclosingFile().equals(c2.f_sourceRef.getEnclosingFile())) {
            String cmt1 = c1.f_sourceRef.getComment();
            String cmt2 = c2.f_sourceRef.getComment();
            if (cmt1 == null) {
              // near match, or completely off
              return (cmt2 == null) ? 1 : Integer.MAX_VALUE;
            }
            if (cmt2 == null) {
              // Completely off since cmt1 != null
              return Integer.MAX_VALUE;
            }
            return c1.f_sourceRef.getOffset() - c2.f_sourceRef.getOffset();
          }
        }
        return Integer.MAX_VALUE;
      }
    };
  }

  public boolean isShallowMatch(ResultsViewContent n) {
    return this.f_baseImageName.equals(n.f_baseImageName) && this.f_imageFlags == n.f_imageFlags && this.f_isInfo == n.f_isInfo
        && this.f_isInfoDecorated == n.f_isInfoDecorated && this.f_isInfoWarning == n.f_isInfoWarning
        && this.f_isInfoWarningDecorate == n.f_isInfoWarningDecorate && this.f_isPromiseWarning == n.f_isPromiseWarning;
  }

  public ResultsViewContent shallowCopy() {
    ResultsViewContent clone;
    try {
      clone = (ResultsViewContent) clone();
      clone.f_children = Collections.emptySet();
      clone.f_getMessage = null; // Invalidate cache
      return clone;
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
  }

  public ResultsViewContent deepCopy() {
    ResultsViewContent copy = shallowCopy();
    copy.f_children = new ArrayList<ResultsViewContent>();
    for (ResultsViewContent c : f_children) {
      final ResultsViewContent copyC = c.deepCopy();
      copy.f_children.add(copyC);
      copyC.setParent(copy);
    }
    return copy;
  }

  public ResultsViewContent getParent() {
    return parent;
  }

  private void setParent(ResultsViewContent p) {
    parent = p;
  }
}