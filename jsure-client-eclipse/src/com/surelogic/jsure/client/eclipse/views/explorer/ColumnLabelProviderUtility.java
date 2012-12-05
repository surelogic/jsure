package com.surelogic.jsure.client.eclipse.views.explorer;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;

import com.surelogic.Utility;
import com.surelogic.common.ui.EclipseColorUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.model.java.Element;
import com.surelogic.jsure.client.eclipse.model.java.ElementDrop;
import com.surelogic.jsure.client.eclipse.model.java.ElementJavaDecl;

@Utility
public final class ColumnLabelProviderUtility {

  private static void highlightRowHelper(ViewerCell cell) {
    if (cell.getElement() instanceof ElementDrop) {
      final ElementDrop element = (ElementDrop) cell.getElement();
      if (element.highlightDifferences()) {
        if (element.isNew() || element.isChanged()) {
          cell.setBackground(EclipseColorUtility.getDiffHighlightColorNewChanged());
          return;
        }
        if (element.isOld()) {
          cell.setBackground(EclipseColorUtility.getDiffHighlightColorObsolete());
          return;
        }
      }
    }
    cell.setBackground(null);
  }

  static final StyledCellLabelProvider TREE = new StyledCellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowHelper(cell);

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

  static final CellLabelProvider POSITION = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowHelper(cell);

      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String line = element.getPositionRelativeToDeclarationAsStringOrNull();
        if (line != null)
          cell.setText(line);
      }
    }
  };

  static final CellLabelProvider LINE = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowHelper(cell);

      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String line = element.getLineNumberAsStringOrNull();
        if (line != null)
          cell.setText(line);
      }
    }
  };

  static final CellLabelProvider DIFF = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
      highlightRowHelper(cell);

      if (cell.getElement() instanceof ElementDrop) {
        final ElementDrop element = (ElementDrop) cell.getElement();
        if (element.getScanDifferences() != null) {
          String cellText = null;
          Image cellImage = null;
          if (element.isNew()) {
            cellImage = SLImages.getGrayscaleImage(element.getElementImage());
            cellText = "New";
          } else if (element.isOld()) {
            cellImage = SLImages.getGrayscaleImage(element.getElementImage());
            cellText = "Obsolete (only in the old scan)";
          } else {
            final IDrop oldDrop = element.getChangedFromDropOrNull();
            if (oldDrop != null) {
              cellImage = SLImages.getGrayscaleImage(element.getElementImageChangedFromDropOrNull());
              cellText = "Changed";
              final String whatChanged = ScanDifferences.getMessageAboutWhatChanged(element.getChangedFromDropOrNull(),
                  element.getDrop());
              if (whatChanged != null) {
                cellText += " to " + whatChanged;
              }
            }
          }
          if (cellImage != null) {
            cell.setImage(cellImage);
          }
          if (cellText != null) {
            cell.setText(cellText);
            cell.setForeground(EclipseColorUtility.getSubtleTextColor());
          }
        }
      }
    }
  };
}
