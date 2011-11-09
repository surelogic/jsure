/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/UnknownAttributeException.java,v 1.8 2003/07/15 21:47:18 aarong Exp $ */
package edu.cmu.cs.fluid.mvc;


/**
 * Runtime exception thrown when an attribute is not found
 * in a particular model.
 */
public class UnknownAttributeException
extends RuntimeException
{
  /**
   * The name of the unknown attribute.
   */
  private final String attr;
  
  /**
   * Create a new exception.
   * @param att The name of the attribtue that was not found.
   */
  public UnknownAttributeException( final String att )
  {
    super( "Attribute  \"" + att + "\" is not found" );
    attr = att;    
  }

  public UnknownAttributeException( final String att, final String model )
  {
    super( "Attribute \"" + att + "\" is not found in model \"" + model + "\"" ); 
    attr = att;    
  }

  public UnknownAttributeException( final String att, final Model model )
  {
    super( "Attribute \"" + att + "\" is not found in model \"" +
           model.getName() + "\"" ); 
    attr = att;    
  }
  
  public String getAttribute()
  {
    return attr;
  }
}

