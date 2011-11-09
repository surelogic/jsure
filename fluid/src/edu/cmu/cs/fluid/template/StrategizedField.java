/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/template/StrategizedField.java,v 1.8 2007/07/05 18:15:16 aarong Exp $ */
package edu.cmu.cs.fluid.template;

import java.util.Vector;

import edu.cmu.cs.fluid.version.VersionTracker;

/**
 * A field that uses a  {@link FieldStrategy} object to 
 * implement the <code>acceptableToField</code> methods.
 */
@Deprecated
@SuppressWarnings("all")
public abstract class StrategizedField
extends Field
{
  /**
   * Value indicating the size of the field is not bounded.
   */
  public static final int NO_LIMIT = FieldStrategy.NO_MAXIMUM_SIZE;

  /**
   * The strategy used by the field.
   */
  private final FieldStrategy strategy;

  /**
   * The value of the field is stored by this vector.
   */
  private Vector value;

  /**
   * The value of the empty property.
   */
  private boolean empty;


  /**
   * Create a new field.
   * @param n The name of the field.
   * @param t The Template to which the field belongs.
   * @param versioned <code>true</code> if the field should be
   * sensitive to versions.
   * @param vt The verion tracker to use to get the version that
   * the data should be from.  (Only interesting if <code>versioned</code>
   * is <code>true</code>.
   * @param c The Field's consultant.
   * @param s The Field's strategy.
   * @exception IllegalArgumentException Thrown if <code>c</code> is <code>null</code>.
   */
  public StrategizedField( final String n, final Template t,
                           final boolean versioned, final VersionTracker vt,
                           final FieldConsultant c, final FieldStrategy s )
  {
    super( n, t, versioned, vt, c );
    strategy = s;
    value = new Vector();
    empty = true;
  }

  /**
   * Create a new field that uses the default field consultant.
   * @param n The name of the field.
   * @param t The Template to which the field belongs.
   * @param versioned <code>true</code> if the field should be
   * sensitive to versions.
   * @param vt The verion tracker to use to get the version that
   * the data should be from.  (Only interesting if <code>versioned</code>
   * is <code>true</code>.
   * @param s The Field's strategy.
   */
  public StrategizedField( final String n, final Template t,
                           final boolean versioned, final VersionTracker vt,
                           final FieldStrategy s )
  {
    this( n, t, versioned, vt, DEFAULT_CONSULTANT, s );
  }



  //--------------------------------------------------------------------
  // New Methods
  //--------------------------------------------------------------------

  /**
   * Get the strategy used by the field
   */
  public FieldStrategy getStrategy()
  {
    return strategy;
  }

  /**
   * Set the value of the field.
   * @param objs The value to place in the field.  Cannot be null.
   * @return <code>true</code> if the field was set successfully,
   * <code>false</code> if 
   * <tt>canAccept( obj )</tt> would return <tt>false</tt>.
   */
  public boolean setValueAsVector( final Vector objs )
  {
    final Object[] array = new Object[objs.size()];
    objs.copyInto( array );
    return setValue( array );
  }

  /**
   * Get the value of the field as a Vector.
   * @return The value of the field as a Vector, or <code>null</code>
   * if the field is empty.
   */
  public Vector getValueAsVector()
  {
    // Lock to prevent clearing of value between isEmpty() and
    // copyInto()
    synchronized( this )
    {
      if( isEmpty() ) {
        return null;
      } else {
        return (Vector)value.clone();
      }
    }
  }

  /**
   * Private setter method for the "empty" property.  Used to
   * encapsulate the sending of PropertyChangedEvents.
   */
  private void setEmpty( final boolean b )
  {
    boolean changed = false;
    synchronized( this )
    {
      if( b != empty )
      {
        empty = b;
        changed = true;
      }
    }
    // This part doesn't need to be synchronized
    // but, it often will be because of the 
    // synchronization of the caller of setEmpty()
    if( changed )
    {
      fireEmptyChanged( b );
    }
  }

  /**
   * Fire a value changed event.
   * @param v The new value of the field as a Vector
   */
  protected void fireValueChanged( Vector v )
  {
    Object[] o = new Object[v.size()];
    v.copyInto( o );
    fireValueChanged( o );
  }



  //--------------------------------------------------------------------
  // Methods from Field
  //--------------------------------------------------------------------

  // Inherit JavaDoc
  @Override
  public boolean acceptableToField( final int pos, final Object obj )
  {
    return strategy.acceptableObject( pos, obj );
  }

  // Inherit JavaDoc
  @Override
  public boolean acceptableToField( final Object[] obj )
  {
    return strategy.acceptableObject( obj );
  }

  // Inherit JavaDoc
  @Override
  public int getSize()
  {
    return value.size();
  }

  // Inherit JavaDoc
  @Override
  public boolean setValue( final Object[] objs )
  {
    if( canAccept( objs ) )
    {
      // Lock to protect value and to make actions atomic
    lock:
      synchronized( this )
      {
        value.setSize( objs.length );
        for( int i = 0; i < objs.length; i++ )
          value.setElementAt( objs[i], i );
        setEmpty( false );
        fireValueChanged( value );
        return true;
      }
    }
    return false;
  }

  // Inherit JavaDoc
  @Override
  public boolean setValue( final int pos, final Object obj )
  {
    if( canAccept( pos, obj ) )
    {
      // Lock to protect value and to make actions atomic
      synchronized( this )
      {
        value.setSize( pos + 1 );
        value.setElementAt( obj, pos );
        setEmpty( false );
        fireValueChanged( value );
      }
      return true;
    }
    return false;
  }

  // Inherit JavaDoc
  @Override
  public Object[] getValue()
  {
    // Lock to prevent clearing of value between isEmpty() and
    // copyInto()
    synchronized( this )
    {
      if( isEmpty() ) {
        return null;
      } else {
        return value.toArray();
      }
    }
  }

  // Inherit JavaDoc
  @Override
  public Object getValue( final int pos )
  {
    // Lock to prevent clearing of value between isEmpty()
    // and getElementAt();
    synchronized( this )
    {
      if( isEmpty() ) {
        return null;
      } else {
        return value.elementAt( pos );
      }
    }
  }

  // Inherit JavaDoc
  @Override
  public boolean insertValue( final int pos, final Object obj )
  {
    boolean canProceed = canAccept( pos, obj );
    for( int i = pos+1; canProceed && i < value.size(); i++ ) {
      canProceed = canAccept( i, value.elementAt( i-1 ) ); 
    }

    if( canProceed )
    {
      // Lock to protect value and to make actions atomic
      synchronized( this )
      {
        value.setSize( pos + 1 );
        value.insertElementAt( obj, pos );
        setEmpty( false );
        fireValueChanged( value );
      }
      return true;
    }
    return false;
  }

  // Inherit JavaDoc
  @Override
  public boolean addValue( final Object obj )
  {
    final boolean canProceed = canAccept( value.size(), obj );

    if( canProceed )
    {
      // Lock to protect value and to make actions atomic
      synchronized( this )
      {
        value.addElement( obj );
        setEmpty( false );
        fireValueChanged( value );
      }
      return true;
    }
    return false;
  }

  // Inherit JavaDoc
  @Override
  public boolean removeValue( final int pos )
  {
    boolean canProceed = true;
    for( int i = pos; canProceed && i < value.size() - 1; i++ ) {
      canProceed = canAccept( i, value.elementAt( i+1 ) ); 
    }

    if( canProceed )
    {
      // Lock to protect value and to make actions atomic
      synchronized( this )
      {
        value.removeElementAt( pos );
        if( value.size() == 0 ) {
          setEmpty( true );
        } else {
          fireValueChanged( value );
        }
        return true;
      }
    }
    return false;
  }

  // Inherit JavaDoc
  @Override
  public boolean isEmpty()
  {
    synchronized( this ) {
      return empty;
    }
  }

  // Inherit JavaDoc
  @Override
  public void clearField()
  {
    // Synchronized to make this atomic
    synchronized( this )
    {
      value.clear();
      setEmpty( true );
    }
  }
}
