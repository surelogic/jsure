/*
 * AttributeAlreadyExistsException.java
 *
 * Created on December 10, 2001, 10:53 AM
 */

package edu.cmu.cs.fluid.mvc;

import com.surelogic.Starts;

/**
 * Exception thrown when an a "fresh" attribute name is expected but the
 * provided attribute name names an already existing attribute.
 *
 * @author Aaron Greenhouse
 */
public class AttributeAlreadyExistsException
extends java.lang.RuntimeException
{
  /** The name of the attribute that already exists. */
  private final String attrName;
  
  /** The name of the Model in which it appears. */
  private final String modelName;
  
  /**
   * Create a new exception.
   * @param attrName The name of the attribute that already exists.
   * @param modelName The name of the Model in which it appears.
   */
  public AttributeAlreadyExistsException( final String attrName,
                                          final String modelName ) 
  {
    this.attrName = attrName;
    this.modelName = modelName;
  }
  
  // inherit Javadoc
  @Starts("nothing")
@Override
  public String getMessage()
  {
    return (  "Attribute \"" + attrName + "\" already exists in model \""
            + modelName + "\".");
  }
}


