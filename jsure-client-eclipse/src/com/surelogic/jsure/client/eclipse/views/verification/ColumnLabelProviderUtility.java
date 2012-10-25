package com.surelogic.jsure.client.eclipse.views.verification;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import com.surelogic.Utility;
import com.surelogic.common.CommonImages;
import com.surelogic.common.ref.DeclUtil;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.ScanDifferences;
import com.surelogic.jsure.client.eclipse.views.ResultsImageDescriptor;

@Utility
public final class ColumnLabelProviderUtility {

  static final StyledCellLabelProvider TREE = new StyledCellLabelProvider() {

    private Color f_changedColor;

    private Color getChangedColor() {
      if (f_changedColor == null) {
        f_changedColor = new Color(Display.getCurrent(), 181, 213, 255);
        Display.getCurrent().disposeExec(new Runnable() {
          public void run() {
            f_changedColor.dispose();
          }
        });
      }
      return f_changedColor;
    }

    private Color f_newColor;

    private Color getNewColor() {
      if (f_newColor == null) {
        f_newColor = new Color(Display.getCurrent(), 213, 255, 181);
        Display.getCurrent().disposeExec(new Runnable() {
          public void run() {
            f_newColor.dispose();
          }
        });
      }
      return f_newColor;
    }

    private Color f_onClauseColor;

    private Color getSpecialColor() {
      if (f_onClauseColor == null) {
        f_onClauseColor = new Color(Display.getCurrent(), 149, 125, 71);
        Display.getCurrent().disposeExec(new Runnable() {
          public void run() {
            f_onClauseColor.dispose();
          }
        });
      }
      return f_onClauseColor;
    }

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final boolean duplicate = hasAncestorWithSameDrop(element);
        String label = element.getLabel();
        if (duplicate)
          label = "\u2191  " + label;

        if (element instanceof ElementAnalysisResultDrop) {
          final Element parent = element.getParent();
          final boolean parentIsAnOrFolder = parent instanceof ElementResultFolderDrop
              && ((ElementResultFolderDrop) parent).getDrop().getLogicOperator() == IResultFolderDrop.LogicOperator.OR;
          if (parentIsAnOrFolder) {
            final String OR = "(or)  ";
            label = OR + label;
            StyleRange[] ranges = { new StyleRange(0, OR.length(), getSpecialColor(), null) };
            cell.setStyleRanges(ranges);
          }
        }
        cell.setText(label);
        cell.setImage(element.getImage());

        if (element instanceof ElementPromiseDrop) {
          int index = label.indexOf(" on ");
          if (index != -1) {
            StyleRange[] ranges = { new StyleRange(index, label.length() - index, getSpecialColor(), null) };
            cell.setStyleRanges(ranges);
          }
        }
        if (element instanceof ElementCategory) {
          if (label.endsWith(")")) {
            int start = label.lastIndexOf('(');
            if (start != -1) {
              StyleRange[] ranges = { new StyleRange(start, label.length() - start, getSpecialColor(), null) };
              cell.setStyleRanges(ranges);
            }
          }
        }
        if (element instanceof ElementProposedPromiseDrop) {
          final String prefixEnd = "promise)";
          int index = label.indexOf(prefixEnd);
          if (index != -1) {
            StyleRange[] ranges = { new StyleRange(0, index + prefixEnd.length(), getSpecialColor(), null) };
            cell.setStyleRanges(ranges);
          }
        }

        final ScanDifferences diff = Element.f_diff;
        if (diff != null && element instanceof ElementDrop) {
          final IDrop drop = ((ElementDrop) element).getDrop();
          if (diff.isNotInOldScan(drop))
            cell.setBackground(getNewColor());
          if (diff.isChangedButInBothScans(drop))
            cell.setBackground(getChangedColor());
        }
      } else
        super.update(cell);
    }

    private boolean hasAncestorWithSameDrop(Element element) {
      if (element instanceof ElementDrop)
        if (((ElementDrop) element).getAncestorWithSameDropOrNull() != null)
          return true;
      return false;
    }
  };

  static abstract class VerificationStatusCellLabelProvider extends CellLabelProvider {
    boolean isNotEmptyOrNull(String value) {
      if (value == null)
        return false;
      if ("".equals(value))
        return false;
      return true;
    }
  }

  static final CellLabelProvider PROJECT = new VerificationStatusCellLabelProvider() {

    private final ResultsImageDescriptor f_projectRid = new ResultsImageDescriptor(CommonImages.IMG_PROJECT, 0,
        VerificationStatusView.ICONSIZE);

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String project = element.getProjectNameOrNull();
        if (isNotEmptyOrNull(project)) {
          cell.setText(project);
          cell.setImage(f_projectRid.getCachedImage());
        }
      }
    }
  };

  static final CellLabelProvider PACKAGE = new VerificationStatusCellLabelProvider() {

    private final ResultsImageDescriptor f_packageRid = new ResultsImageDescriptor(CommonImages.IMG_PACKAGE, 0,
        VerificationStatusView.ICONSIZE);

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final String pkg = element.getPackageNameOrNull();
        if (isNotEmptyOrNull(pkg)) {
          cell.setText(pkg);
          cell.setImage(f_packageRid.getCachedImage());
        }
      }
    }
  };

  static final CellLabelProvider TYPE = new VerificationStatusCellLabelProvider() {

    private final ResultsImageDescriptor f_classRid = new ResultsImageDescriptor(CommonImages.IMG_CLASS, 0,
        VerificationStatusView.ICONSIZE);
    private final ResultsImageDescriptor f_interfaceRid = new ResultsImageDescriptor(CommonImages.IMG_INTERFACE, 0,
        VerificationStatusView.ICONSIZE);
    private final ResultsImageDescriptor f_enumRid = new ResultsImageDescriptor(CommonImages.IMG_ENUM, 0,
        VerificationStatusView.ICONSIZE);
    private final ResultsImageDescriptor f_annotationRid = new ResultsImageDescriptor(CommonImages.IMG_ANNOTATION, 0,
        VerificationStatusView.ICONSIZE);

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof ElementDrop) {
        final ElementDrop element = (ElementDrop) cell.getElement();
        final String typeName = element.getSimpleTypeNameOrNull();
        if (isNotEmptyOrNull(typeName)) {
          cell.setText(typeName);
          IJavaRef ref = element.getDrop().getJavaRef();
          if (ref == null)
            cell.setImage(f_classRid.getCachedImage());
          else {
            switch (DeclUtil.getTypeKind(ref.getDeclaration())) {
            case ANNOTATION:
              cell.setImage(f_annotationRid.getCachedImage());
              break;
            case ENUM:
              cell.setImage(f_enumRid.getCachedImage());
              break;
            case CLASS:
              cell.setImage(f_classRid.getCachedImage());
              break;
            case INTERFACE:
              cell.setImage(f_interfaceRid.getCachedImage());
              break;
            default:
              cell.setImage(f_classRid.getCachedImage());
            }
          }
        }
      }
    }
  };

  static final CellLabelProvider LINE = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {
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
      if (cell.getElement() instanceof Element) {
        final Element element = (Element) cell.getElement();
        final ScanDifferences diff = Element.f_diff;
        if (diff != null && element instanceof ElementDrop) {
          final IDrop drop = ((ElementDrop) element).getDrop();
          if (diff.isNotInOldScan(drop))
            cell.setText("New");
          final IDrop oldDrop = diff.getChangedInOldScan(drop);
          if (oldDrop != null) {
            cell.setText(oldDrop.getMessage());
          }
        }
      }
    }
  };
}
