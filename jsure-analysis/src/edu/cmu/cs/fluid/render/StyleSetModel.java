package edu.cmu.cs.fluid.render;

import javax.swing.Icon;

import edu.cmu.cs.fluid.mvc.set.SetModel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.ExplicitSlotFactory;

/**
 * A SetModel with nodes representing rendering styles.
 * Each style has the following:
 *   A name
 *   An HTML tag for any text/label
 *   An Icon        (if no icon is present)
 *   a String label (otherwise)
 *   Horizontal and vertical alignment/position of the text
 *
 * @author Edwin Chan
 */
public interface StyleSetModel extends SetModel {
  //===========================================================
  //== Descriptors
  //===========================================================
  public static final String CHOOSE_COLOR = "StyleSetModel.chooseColor";

  //===========================================================
  //== Attribute names
  //===========================================================
  /** 
   * The name of the style (if any)
   */
  public static final String NAME = "StyleSetModel.name";

  /** 
   * The HTML tag for styling any text -- unused
   */
  public static final String HTML_TAG = "StyleSetModel.htmlTag";

  /** 
   * The Icon used (if no icon is already used)
   */
  public static final String ICON = "StyleSetModel.icon";

  /** 
   * The name of the corresponding Icon used
   */
  public static final String ICON_NAME = "StyleSetModel.iconName";

  /** 
   * The label used (if no text is available)
   */
  public static final String LABEL = "StyleSetModel.label";

  /** 
   * The horizontal alignment of any text (relative to any icon)
   */
  public static final String H_ALIGNMENT = "StyleSetModel.h_alignment";

  /** 
   * The horizontal position of any text 
   */
  public static final String H_POSITION = "StyleSetModel.h_position";

  /** 
   * The vertical alignment of any text (relative to any icon)
   */
  public static final String V_ALIGNMENT = "StyleSetModel.v_alignment";

  /** 
   * The vertical position of any text 
   */
  public static final String V_POSITION = "StyleSetModel.v_position";

  /** 
   * The color of any text (as a 6 digit hex number)
   */
  public static final String COLOR = "StyleSetModel.color";

  public void setStyleName( IRNode node, String s );
  public String getStyleName( IRNode node );

  public void setHtmlTag( IRNode node, String s );
  public String getHtmlTag( IRNode node );

  public void setIcon( IRNode node, Icon i );
  public Icon getIcon( IRNode node );

  public void setLabel( IRNode node, String s );
  public String getLabel( IRNode node );

  public void setHorizontalTextAlignment( IRNode node, int i );
  public int getHorizontalTextAlignment( IRNode node );
 
  public void setHorizontalTextPosition( IRNode node, int i );
  public int getHorizontalTextPosition( IRNode node );

  public void setVerticalTextAlignment( IRNode node, int i );
  public int getVerticalTextAlignment( IRNode node );

  public void setVerticalTextPosition( IRNode node, int i );
  public int getVerticalTextPosition( IRNode node );

  public void setColor( IRNode node, String s );
  public String getColor( IRNode node );

  public void setIconName( IRNode node, String s );
  public String getIconName( IRNode node ); 

  public static interface Factory {
    StyleSetModel create (String name, ExplicitSlotFactory sf)
      throws SlotAlreadyRegisteredException;
  }
}
