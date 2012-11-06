package com.surelogic.jsure.client.eclipse;

import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeColumn;

import com.surelogic.Utility;
import com.surelogic.common.core.EclipseUtility;

@Utility
public final class JSureClientUtility {

  private static Color f_subtleTextColor;

  public static Color getSubtleTextColor() {
    if (f_subtleTextColor == null) {
      f_subtleTextColor = new Color(Display.getCurrent(), 149, 125, 71);
      Display.getCurrent().disposeExec(new Runnable() {
        public void run() {
          f_subtleTextColor.dispose();
        }
      });
    }
    return f_subtleTextColor;
  }

  private static Color f_diffHighlightColorNewChanged;

  public static Color getDiffHighlightColorNewChanged() {
    if (f_diffHighlightColorNewChanged == null) {
      f_diffHighlightColorNewChanged = new Color(Display.getCurrent(), 255, 255, 190);
      Display.getCurrent().disposeExec(new Runnable() {
        public void run() {
          f_diffHighlightColorNewChanged.dispose();
        }
      });
    }
    return f_diffHighlightColorNewChanged;
  }

  private static Color f_diffHighlightColorObsolete;

  public static Color getDiffHighlightColorObsolete() {
    if (f_diffHighlightColorObsolete == null) {
      f_diffHighlightColorObsolete = new Color(Display.getCurrent(), 255, 190, 190);
      Display.getCurrent().disposeExec(new Runnable() {
        public void run() {
          f_diffHighlightColorObsolete.dispose();
        }
      });
    }
    return f_diffHighlightColorObsolete;
  }

  /**
   * Utility class used to persist column widths based upon the use's
   * preference.
   */
  public static class ColumnResizeListener extends ControlAdapter {

    final String f_prefKey;

    /**
     * Constructs an instance.
     * 
     * @param prefKey
     *          the preference key to use when calling
     *          {@link EclipseUtility#setIntPreference(String, int)} to persist
     *          the width of this column.
     */
    public ColumnResizeListener(String prefKey) {
      f_prefKey = prefKey;
    }

    @Override
    public void controlResized(ControlEvent e) {
      if (e.widget instanceof TreeColumn) {
        int width = ((TreeColumn) e.widget).getWidth();
        EclipseUtility.setIntPreference(f_prefKey, width);
      }
    }
  }

  private JSureClientUtility() {
    // no instances
  }
}
