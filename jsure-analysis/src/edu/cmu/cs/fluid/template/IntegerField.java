/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/IntegerField.java,v 1.7 2005/06/13 19:04:11 chance Exp $ */
package edu.cmu.cs.fluid.template;


/**
 * Field that holds a Java Integer.
 */
@Deprecated
@SuppressWarnings("all")
public class IntegerField
extends StrategizedField
{
  /**
   * Create a new field with a given name and parent Template.
   * @param n The name of the field.
   * @param t The Template to which the field belongs.
   * @param c The Field's consultant.
   * @exception IllegalArgumentException Thrown if <code>c</code> is <code>null</code>.
   */
  public IntegerField( final String n, final Template t,
                       final FieldConsultant c )
  {
    super( n, t, false, null, c, IntegerStrategy.prototype );
  }

  /**
   * Create a new field with a given name and parent Template.
   * @param n The name of the field.
   * @param t The Template to which the field belongs.
   */
  public IntegerField( final String n, final Template t )
  {
    this( n, t, DEFAULT_CONSULTANT );
  }



  //--------------------------------------------------------------------
  // Convienence Methods
  //--------------------------------------------------------------------

  /**
   * Get the Integer stored in the field.  This is the 
   * same as <tt>(Integer)getValue( 0 )</TT>.
   * @return The Integer stored in the field, or <code>null</code>
   * if the field is empty.
   */
  public Integer getValueAsInteger()
  {
    return (Integer)getValue( 0 );
  }

  /**
   * Set the Integer stored in the field.  This is the 
   * same as <tt>setValue( 0, i )</TT>.
   * @param i The Integer to store in the field.
   * @return <code>true</code> if the field was set successfully,
   * or false if the 
   * field's consultant rejects the Integer <code>b</code>.
   */
  public boolean setValueAsInteger( Integer i )
  {
    return setValue( 0, i );
  }

  /**
   * Set the Integer stored in the field.  This is the 
   * same as <tt>setValue( 0, new Integer( i ) )</TT>.
   * @param i The int to store in the field.
   * @return <code>true</code> if the field was set successfully,
   * or false if the 
   * field's consultant rejects the Integer <code>b</code>.
   */
  public boolean setValueAsInteger( int i )
  {
    return setValueAsInteger( new Integer( i ) );
  }
}
