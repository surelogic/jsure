package com.surelogic.jsure.views.debug.oracleDiff;

import java.util.logging.Logger;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.xml.results.coe.ResultsTreeNode;

import edu.cmu.cs.fluid.dcf.views.coe.IResultsViewContentProvider;
import edu.cmu.cs.fluid.dcf.views.coe.IResultsViewLabelProvider;
import edu.cmu.cs.fluid.dcf.views.coe.ResultsView;
import edu.cmu.cs.fluid.dcf.views.coe.TreeNodeResultsViewLabelProvider;

public class OracleDiffResultsView extends ResultsView {
  private static final Logger LOG = SLLogger.getLogger("OracleDiffResultsView");

  @Override
  protected IResultsViewContentProvider makeContentProvider() {
    return new OracleDiffResultsViewContentProvider();
  }
  
  @Override
  protected IResultsViewLabelProvider makeLabelProvider() {
    return new TreeNodeResultsViewLabelProvider();
  }
  
  @Override
  protected ViewerSorter createSorter() {
    return new TreeNodeNameSorter();
  }
  
  
  
  public static class TreeNodeNameSorter extends ViewerSorter {
    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      int result; // = super.compare(viewer, e1, e2);
      boolean bothTreeNode = (e1 instanceof ResultsTreeNode)
          && (e2 instanceof ResultsTreeNode);
      if (bothTreeNode) {
        final ResultsTreeNode n1 = (ResultsTreeNode) e1;
        final ResultsTreeNode n2 = (ResultsTreeNode) e2;
        result = n1.msg.compareTo(n2.msg);
        if (result == 0) {
          if (n1.source != null && n2.source != null) {
            result = n1.source.compareTo(n2.source);
            if (result == 0) {
              if (n1.line != null && n2.line != null) {
                final int l1 = Integer.valueOf(n1.line);
                final int l2 = Integer.valueOf(n2.line);
                result = (l1 == l2) ? 0 : ((l1 < l2) ? -1 : 1);
              }
            }
          }
        }
      } else {
        LOG.warning("e1 and e2 are not ResultsTreeNode objects: e1 = \""
            + e1.toString() + "\"; e2 = \"" + e2.toString() + "\"");
        return -1;
      }

      return result;
    }
  }

}
