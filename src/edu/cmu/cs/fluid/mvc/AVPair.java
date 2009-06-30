// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AVPair.java,v 1.8 2007/06/04 16:55:01 aarong Exp $

package edu.cmu.cs.fluid.mvc;


/**
 * An immutable class storing an attribute&ndash;value pair.
 */
public final class AVPair
implements Cloneable
{
  //===========================================================
  //== Fields
  //===========================================================

  /**
   * The name of the attribute.  This must be an 
   * {@link java.lang.String#intern}ed <code>String</code>.
   */
  private final String attribute;

  /** The value */
  private final Object value;


  
  //===========================================================
  //== Constructor
  //===========================================================

  /**
   * Create a new pair.
   */
  public AVPair( final String att, final Object val )
  {
    attribute = att.intern();
    value = val;
  }



  //===========================================================
  //== Static Methods
  //===========================================================

  /**
   * Find the first pair in an array of pairs whose attribute component
   * matches the given attribute name. 
   * @return The first pair in the array to match, or <code>null</code>
   * if no such element exists.
   */
  public static AVPair findAttribute( final AVPair[] pairs, final String att )
  {
    return findAttributeInterned( pairs, att.intern() );
  }

  /**
   * Find the first pair in an array of pairs whose attribute component
   * matches the given attribute name. 
   * <em>The attribute <code>att</code> must be an interned
   * <code>String</code>.</em>  It is the caller's responsiblity
   * to insure that this is the case.
   * @return The first pair in the array to match, or <code>null</code>
   * if no such element exists.
   */
  public static AVPair findAttributeInterned(
    final AVPair[] pairs, final String att )
  {
    AVPair pair = null;
    for( int i = 0; (i < pairs.length) && (pair == null); i++ ) {
      if( pairs[i].attribute == att ) pair = pairs[i];
    }
    return pair;
  }



  //===========================================================
  //== Getter/setter methods
  //===========================================================

  /** 
   * Get the attribute.  This always returns an 
   * {@link java.lang.String#intern}ed <code>String</code>.
   */
  public String getAttribute()
  {
    return attribute;
  }

  /**
   * Get the value;
   */
  public Object getValue()
  {
    return value;
  }

  /**
   * Change the value.  Returns a new AVPair with the same
   * attribute, but the new value.
   */
  public AVPair setValue( final Object newVal )
  {
    return new AVPair( attribute, newVal );
  }



  //===========================================================
  //== Override methods from object
  //===========================================================

  /**
   * Check equality.  Two pairs are equal if their attributes and values
   * are equal.
   */
  @Override
  public boolean equals( final Object o )
  {
    if (o instanceof AVPair) {
      final AVPair p = (AVPair)o;
      return (attribute == p.attribute) && value.equals( p.value );
    } else {
      return false;
    }
  }

  /**
   * Get the hashcode.  The hashcode is the xor of the attribute and
   * value hashcodes.
   */
  @Override
  public int hashCode()
  {
    return attribute.hashCode() ^ value.hashCode();
  }

  /**
   * Clone the pair.
   */
  @Override
  public Object clone()
  {
    return this;
  }

  /**
   * Convert to a String representation.  The representation is
   * equivalent to <code>"(" + getAttribute() + ", " + getValue() + ")"</code>.
   */
  @Override
  public String toString()
  {
    return "(" + getAttribute() + ", " + getValue() + ")";
  }
}
