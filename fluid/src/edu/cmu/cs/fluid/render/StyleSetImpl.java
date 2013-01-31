package edu.cmu.cs.fluid.render;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import edu.cmu.cs.fluid.mvc.AttributeValuesChangedEvent;
import edu.cmu.cs.fluid.mvc.ModelCore;
import edu.cmu.cs.fluid.mvc.set.AbstractSetModel;
import edu.cmu.cs.fluid.mvc.set.SetModelCore;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.IntegerTable;

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
public class StyleSetImpl extends AbstractSetModel 
  implements SwingConstants, StyleSetModel {
  /**
   * A list of all the attributes defined in this model
   */
  static final String[] attrNames = {
    NAME, HTML_TAG, ICON, LABEL, 
    H_ALIGNMENT, H_POSITION, V_ALIGNMENT, V_POSITION, COLOR, ICON_NAME
  };

  /**
   * A list of the types of all the attributes listed in attrNames
   */
  static final IRType[]     types = { IRStringType.prototype,
			       IRStringType.prototype,
			       IconType.prototype, 
			       IRStringType.prototype,

			       IRIntegerType.prototype, 
			       IRIntegerType.prototype, 
			       IRIntegerType.prototype, 
			       IRIntegerType.prototype,
			       IRStringType.prototype,
			       IRStringType.prototype,
  };

  final SlotInfo[]   attrs = new SlotInfo[attrNames.length];

  //===========================================================
  //== Attribute Convenience Methods
  //===========================================================
  private void setValue( int i, final IRNode node, final Object o ) {
    synchronized( structLock ) {
      node.setSlotValue( attrs[i], o );
    }
    modelCore.fireModelEvent( new AttributeValuesChangedEvent( this, node, attrNames[i], o) );
  }
  private Object getValue( int i, final IRNode node ) {
    synchronized( structLock ) {
      return node.getSlotValue( attrs[i] );
    }
  }

  @Override
  public void setStyleName( final IRNode node, final String s ) {
    setValue(0, node, s);
  }
  @Override
  public String getStyleName( final IRNode node ) {
    return (String) getValue(0, node);
  }

  @Override
  public void setHtmlTag( final IRNode node, final String s ) {
    setValue(1, node, s);
  }
  @Override
  public String getHtmlTag( final IRNode node ) {
    return (String) getValue(1, node);
  }

  @Override
  public void setIcon( final IRNode node, final Icon i ) {
    setValue(2, node, i);
  }
  @Override
  public Icon getIcon( final IRNode node ) {
    return (Icon) getValue(2, node);
  }

  @Override
  public void setLabel( final IRNode node, final String s ) {
    setValue(3, node, s);
  }
  @Override
  public String getLabel( final IRNode node ) {
    return (String) getValue(3, node);
  }

  @Override
  public void setHorizontalTextAlignment( IRNode node, int i ) {
    setValue(4, node, IntegerTable.newInteger(i));
  }

  @Override
  public int getHorizontalTextAlignment( IRNode node ) {
    return ((Integer) getValue(4, node)).intValue();
  }

  @Override
  public void setHorizontalTextPosition( IRNode node, int i ) {
    setValue(5, node, IntegerTable.newInteger(i));
  }

  @Override
  public int getHorizontalTextPosition( IRNode node ) {
    return ((Integer) getValue(5, node)).intValue();
  }

  @Override
  public void setVerticalTextAlignment( IRNode node, int i ) {
    setValue(6, node, IntegerTable.newInteger(i));
  }

  @Override
  public int getVerticalTextAlignment( IRNode node ) {
    return ((Integer) getValue(6, node)).intValue();
  }

  @Override
  public void setVerticalTextPosition( IRNode node, int i ) {
    setValue(7, node, IntegerTable.newInteger(i));
  }

  @Override
  public int getVerticalTextPosition( IRNode node ) {
    return ((Integer) getValue(7, node)).intValue();
  }

  @Override
  public void setColor( final IRNode node, final String s ) {
    setValue(8, node, s);
  }
  @Override
  public String getColor( final IRNode node ) {
    // debug("Getting color for style "+node);
    return (String) getValue(8, node);
  }

  @Override
  public void setIconName( final IRNode node, final String s ) {
    setValue(9, node, s);
  }
  @Override
  public String getIconName( final IRNode node ) {
    return (String) getValue(9, node);
  }

  //===========================================================
  //== Constructors
  //===========================================================

  protected StyleSetImpl( final String name, 
			  final SlotFactory sf, 
			  final ModelCore.Factory mf,
			  final SetModelCore.Factory smf )
  throws SlotAlreadyRegisteredException
  {    
    super( name, mf, smf, sf );
      
    for(int i = 0; i<attrs.length; i++) {
      attrs[i] = addNodeAttribute(attrNames[i], types[i], sf, true );
    }
  }
}
