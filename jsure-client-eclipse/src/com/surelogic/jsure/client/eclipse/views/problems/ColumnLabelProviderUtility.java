package com.surelogic.jsure.client.eclipse.views.problems;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;

import com.surelogic.Utility;
import com.surelogic.common.ui.EclipseColorUtility;
import com.surelogic.jsure.client.eclipse.model.java.Element;
import com.surelogic.jsure.client.eclipse.model.java.ElementDrop;
import com.surelogic.jsure.client.eclipse.model.java.ElementJavaDecl;

@Utility
public final class ColumnLabelProviderUtility {

  static final StyledCellLabelProvider TREE = new StyledCellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      ElementDrop.highlightRowHelper(cell);

      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        String label = element.getLabel();

        if (element instanceof ElementJavaDecl) {
          /*
           * Match Eclipse with uses subtle text color for ": Object" (types)
           */
          final int colonIndex = label.indexOf(':');
          if (colonIndex != -1) {
            StyleRange[] ranges = { new StyleRange(colonIndex, label.length(), EclipseColorUtility.getSubtleTextColor(), null) };
            cell.setStyleRanges(ranges);
          }
        }

        cell.setText(label);
        cell.setImage(element.getImage());

      } else
        super.update(cell);
    }
  };

  static final CellLabelProvider LINE = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      ElementDrop.highlightRowHelper(cell);

      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String line = element.getLineNumberAsStringOrNull();
        if (line != null)
          cell.setText(line);
      }
    }
  };
}
