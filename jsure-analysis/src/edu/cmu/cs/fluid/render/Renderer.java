// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/render/Renderer.java,v 1.11
// 2003/07/15 18:39:11 thallora Exp $
package edu.cmu.cs.fluid.render;

import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JMenu;

import com.surelogic.common.logging.SLLogger;

public interface Renderer {
  public static final Logger RENDER = SLLogger.getLogger("RENDER");

  /**
	 * Get the Component for the renderer
	 */
  public JComponent getComponent();
  public JComponent getToolbar();
  public JMenu getMenu();

  public void setFont(Font f);

  public static final String NO_ELLIPSIS = "No ellipsis";
  public static final String ELLIPSIS_AT_TOP = "Ellipsis at the top";
  public static final String ELLIPSIS_AT_BOTTOM = "Ellipsis at the bottom";
  public static final String MULTIPLE_ELLIPSES = "Multiple ellipses";
}