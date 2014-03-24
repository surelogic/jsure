package com.surelogic.jsure.views.debug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IDrop;

public abstract class AbstractResultsTableContentProvider<T extends IDrop> implements IResultsTableContentProvider {
  private final List<T> f_contents = new ArrayList<T>();
  private final String[] f_labels;

  protected final Comparator<T> sortAsString = new Comparator<T>() {
    @Override
    public int compare(T d1, T d2) {
      return d1.toString().compareTo(d2.toString());
    }
  };

  protected final Comparator<IDrop> sortByLocation = new Comparator<IDrop>() {
    @Override
    public int compare(IDrop d1, IDrop d2) {
      IJavaRef ref = d1.getJavaRef();
      final String tn1 = ref == null ? "" : ref.getTypeNameFullyQualified();
      ref = d2.getJavaRef();
      final String tn2 = ref == null ? "" : ref.getTypeNameFullyQualified();
      int rv = 0;
      // Make those with a real path go first
      if (tn1.startsWith("/")) {
        rv = -1;
      }
      if (tn2.startsWith("/")) {
        rv++;
      }
      if (rv == 0) {
        // Make those with no path go last
        if (tn1.length() == 0) {
          rv = 1;
        }
        if (tn2.length() == 0) {
          rv--;
        }
      }
      if (rv == 0) {
        rv = tn1.compareTo(tn2);
      }
      if (rv == 0) {
        int line1 = getLine(d1);
        int line2 = getLine(d2);
        if (line1 == -1 && line2 == -1) {
          // same, leave rv at 0
        } else if (line1 == -1)
          rv = 1;
        else if (line2 == -1)
          rv = -1;
        else
          rv = line1 - line2;
      }
      if (rv == 0) {
        rv = d1.getMessage().compareTo(d2.getMessage());
      }
      return rv;
    }
  };

  public AbstractResultsTableContentProvider(String[] labels) {
    f_labels = labels;
  }

  public AbstractResultsTableContentProvider(String mainLabel) {
    this(new String[] { mainLabel, "Resource", "Line" });
  }

  @Override
  public final String[] getColumnLabels() {
    return f_labels;
  }

  @Override
  public final int numColumns() {
    return f_labels.length;
  }

  @Override
  public final String getColumnTitle(int column) {
    return f_labels[column];
  }

  @Override
  public boolean isIntSortedColumn(int colIdx) {
    return colIdx == 2;
  }

  @Override
  public int getColumnWeight(int column) {
    switch (column) {
    case 0:
      return 70;
    case 1:
      return 20;
    case 2:
      return 10;
    }
    return 10;
  }

  @Override
  public Object[] getElements(Object inputElement) {
    return f_contents.toArray();
  }

  @Override
  public String build() {
    f_contents.clear();
    return getAndSortResults(f_contents);
  }

  protected abstract String getAndSortResults(List<T> mutableContents);

  protected String getMainColumnText(T d) {
    return d.getMessage();
  }

  @Override
  @SuppressWarnings("unchecked")
  public String getColumnText(Object element, int columnIndex) {
    T d = (T) element;
    switch (columnIndex) {
    case 0:
      return getMainColumnText(d);
    case 1:
      IJavaRef ref = d.getJavaRef();
      final String typeName = ref == null ? "" : ref.getTypeNameFullyQualified();
      return typeName;
    case 2:
      int line = getLine(d);
      if (line > 0) {
        return Integer.toString(line);
      } else if (line >= 0) {
        return "(binary)";
      }
    }
    return "";
  }

  @Override
  public final void dispose() {
    f_contents.clear();
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // throw new UnsupportedOperationException();
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    // throw new UnsupportedOperationException();
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    // throw new UnsupportedOperationException();
  }

  /**
   * Gets the line number of the code snippet to which the passed drop refers
   * to, or -1 if unknown.
   * 
   * @param d
   *          a drop
   * @return the line number of the code snippet to which the passed drop refers
   *         to, or -1 if unknown.
   */
  private static int getLine(IDrop d) {
    IJavaRef ref = d.getJavaRef();
    if (ref != null) {
      return ref.getLineNumber();
    }
    return -1;
  }
}
