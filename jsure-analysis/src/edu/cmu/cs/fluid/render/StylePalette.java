package edu.cmu.cs.fluid.render;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.AVPair;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.SimpleComponentSlotFactory;
import edu.cmu.cs.fluid.mvc.set.SetModelCore;
import edu.cmu.cs.fluid.render.util.IconMap;
import edu.cmu.cs.fluid.util.IntegerTable;

public final class StylePalette implements SwingConstants {
  static void debug(String message) {
    Renderer.RENDER.fine(message);
  }

  /**
	 * Create a new StylePalette that is of variable size.
	 */
  public static StyleSetModel newPalette(
    final String name,
    final SlotFactory sf)
    throws SlotAlreadyRegisteredException {
    final ModelCore.Factory mf = new ModelCore.StandardFactory(sf);
    final SetModelCore.Factory smf =
      new SetModelCore.StandardFactory(
        sf,
        SimpleComponentSlotFactory.simplePrototype);
    return new StyleSetImpl(name, sf, mf, smf);
  }

  private static final String defaultName = "Unnamed";

  /** Style count for creating unique style names */
  private static int styleCount = 1;

  /**
	 * Default values for the slots
	 */
  private static final AVPair[] avps =
    new AVPair[] { null, null, // new AVPair(HTML_TAG, "<B>"),
    null, // COLOR
    null, // ICON
    null, // ICON_NAME
    new AVPair(StyleSetModel.LABEL, ""),

    // LEFT, CENTER, RIGHT, LEADING, TRAILING
    new AVPair(StyleSetModel.H_ALIGNMENT, IntegerTable.newInteger(LEADING)),
      new AVPair(StyleSetModel.H_POSITION, IntegerTable.newInteger(TRAILING)),

    // TOP, CENTER, BOTTOM
    new AVPair(StyleSetModel.V_ALIGNMENT, IntegerTable.newInteger(CENTER)),
      new AVPair(StyleSetModel.V_POSITION, IntegerTable.newInteger(CENTER)),
      };

  static String getNewName() {
    String name = defaultName + "_" + styleCount;
    styleCount++;
    return name;
  }

  /**
	 * Create a default style, with a random color, and pseudo-random name
	 */
  public static IRNode getNewStyle(StyleSetModel ssm) {
    IRNode n = getNewStyle(ssm, getNewName());
    return n;
  }

  /**
	 * Create a default style, with a random color, but with the given name
	 */
  public static IRNode getNewStyle(StyleSetModel ssm, String name) {
    return createStyle(ssm, name, computeColor(), "yball.gif");
  }

  private static final Random randGen = new Random(1973);
  private static final String[] quanta = { "00", "33", "66", "99", "cc" };
  private static final int maxColors =
    quanta.length * quanta.length * quanta.length;

  /**
	 * Pick each each color component out of quanta
	 */
  private static String computeColor() {
    String color = "0x";
    int base = randGen.nextInt(maxColors);

    while (base == 0 || base == maxColors - 1) {
      base = randGen.nextInt(maxColors);
    }
    for (int i = 0; i < 3; i++) {
      color += quanta[base % quanta.length];
      base /= quanta.length;
    }
    debug(color);
    return color.intern();
  }

  /**
	 * Create a new style, based on the original styles Currently implemented as
	 * returning the first valid style
	 */
  public static IRNode mergeStyles(StyleSetModel ssm, List styles) {
    for (int i = 0; i < styles.size(); i++) {
      IRNode style = (IRNode) styles.get(i);
      if (style != null && ssm.isPresent(style)) {
        return style;
      }
    }
    return null;
  }

  /**
	 * Create a new style with the following
	 */
  public static IRNode createStyle(
    final StyleSetModel ssm,
    final String name,
    final String color,
    final String iconName) {
    debug(
      "Creating style '"
        + name
        + "' with color '"
        + color
        + "' and icon '"
        + iconName
        + "'");
    IRNode n = new MarkedIRNode("SSM-" + ssm.getName());

    final Icon icon = getIcon(iconName);
    final String tag = "<font color=" + color + ">";

    avps[0] = new AVPair(StyleSetModel.NAME, name.intern());
    avps[1] = new AVPair(StyleSetModel.HTML_TAG, tag.intern());
    avps[2] = new AVPair(StyleSetModel.COLOR, color.intern());
    avps[3] = new AVPair(StyleSetModel.ICON, icon);
    avps[4] = new AVPair(StyleSetModel.ICON_NAME, iconName.intern());
    ssm.addNode(n, avps);
    return n;
  }

  public static IRNode findStyle(StyleSetModel ssm, String name) {
    Iterator it = ssm.getNodes();
    while (it.hasNext()) {
      IRNode n = (IRNode) it.next();
      String s = ssm.getStyleName(n);
      if (name.equals(s)) {
        return n;
      }
    }
    return null;
  }

  public static Icon getIcon(final String iconName) {
    return IconMap.prototype.getIcon(iconName);
  }
}
