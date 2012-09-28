package com.surelogic.jsure.client.eclipse.views;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.Viewer;

import com.surelogic.dropsea.IDrop;

import edu.cmu.cs.fluid.java.ISrcRef;

public abstract class AbstractResultsTableContentProvider<T extends IDrop> implements IResultsTableContentProvider {
  private final List<T> f_contents = new ArrayList<T>();
  private final String[] f_labels;

  protected final Comparator<T> sortAsString = new Comparator<T>() {
    public int compare(T d1, T d2) {
      return d1.toString().compareTo(d2.toString());
    }
  };

  protected final Comparator<IDrop> sortByLocation = new Comparator<IDrop>() {
    public int compare(IDrop d1, IDrop d2) {
      ISrcRef ref = d1.getSrcRef();
      final String res1 = ref == null ? "" : ref.getRelativePath();
      ref = d2.getSrcRef();
      final String res2 = ref == null ? "" : ref.getRelativePath();
      int rv = 0;
      // Make those with a real path go first
      if (res1.startsWith("/")) {
        rv = -1;
      }
      if (res2.startsWith("/")) {
        rv++;
      }
      if (rv == 0) {
        // Make those with no path go last
        if (res1.length() == 0) {
          rv = 1;
        }
        if (res2.length() == 0) {
          rv--;
        }
      }
      if (rv == 0) {
        rv = res1.compareTo(res2);
      }
      if (rv == 0) {
        rv = getLine(d1) - getLine(d2);
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

  public final String[] getColumnLabels() {
    return f_labels;
  }

  public final int numColumns() {
    return f_labels.length;
  }

  public final String getColumnTitle(int column) {
    return f_labels[column];
  }

  public boolean isIntSortedColumn(int colIdx) {
    return colIdx == 2;
  }

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

  public Object[] getElements(Object inputElement) {
    return f_contents.toArray();
  }

  public String build() {
    f_contents.clear();
    return getAndSortResults(f_contents);
  }

  protected abstract String getAndSortResults(List<T> mutableContents);

  protected String getMainColumnText(T d) {
    return d.getMessage();
  }

  @SuppressWarnings("unchecked")
  public String getColumnText(Object element, int columnIndex) {
    T d = (T) element;
    switch (columnIndex) {
    case 0:
      return getMainColumnText(d);
    case 1:
      ISrcRef ref = d.getSrcRef();
      final String relPath = ref == null ? "" : ref.getRelativePath();
      return relPath;
    case 2:
      int line = getLine(d);
      if (line > 0 && line < Integer.MAX_VALUE) {
        return Integer.toString(line);
      } else if (line >= 0) {
        return "(binary)";
      }
    }
    return "";
  }

  public final void dispose() {
    f_contents.clear();
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    // throw new UnsupportedOperationException();
  }

  public boolean isLabelProperty(Object element, String property) {
    throw new UnsupportedOperationException();
  }

  public void addListener(ILabelProviderListener listener) {
    // throw new UnsupportedOperationException();
  }

  public void removeListener(ILabelProviderListener listener) {
    // throw new UnsupportedOperationException();
  }

  private static int getLine(IDrop d) {
    ISrcRef ref = d.getSrcRef();
    if (ref != null) {
      return ref.getLineNumber();
    }
    return Integer.MAX_VALUE;
  }
}
