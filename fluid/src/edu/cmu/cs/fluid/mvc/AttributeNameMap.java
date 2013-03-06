/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/AttributeNameMap.java,v 1.5 2003/07/15 18:39:10 thallora Exp $ */
package edu.cmu.cs.fluid.mvc;

  
  

/**
 * Interface for mapping the name of an inherited attribute to the
 * name the attribute should have in the inheriting model.
 */
public interface AttributeNameMap
{
  /**
   * The singleton prototype instance of the identity map.
   */
  public static final AttributeNameMap ID_MAP = new IdentityAttributeNameMap();

  /**
   * Map a model-level attribute name to a new local attribute name.
   * @param srcName The name the attribute has in the model
   *                from which it is being inherited as an interned String.
   * @return The name the attribute should have in the inheriting model
   *         as an interned String.
   */
  public String mapCompAttribute( String srcName );

  /**
   * Map a node-level attribute name to a new local attribute name.
   * @param srcName The name the attribute has in the model
   *                from which it is being inherited as an interned String.
   * @return The name the attribute should have in the inheriting model
   *         as an interned String.
   */
  public String mapNodeAttribute( String srcName );
}



/**
 * An attribute name mapper that is the identity function.
 */
final class IdentityAttributeNameMap
implements AttributeNameMap
{
  /**
   * Returns the input String.
   */
  @Override
  public String mapCompAttribute( final String srcName )
  {
    return srcName;
  }
  
  /**
   * Returns the input String.
   */
  @Override
  public String mapNodeAttribute( final String srcName )
  {
    return srcName;
  }
}

