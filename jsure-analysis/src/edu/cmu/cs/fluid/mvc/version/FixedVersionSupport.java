/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/mvc/version/FixedVersionSupport.java,v 1.5 2003/07/15 21:47:18 aarong Exp $
 *
 * FixedVersionSupport.java
 * Created on February 26, 2002, 3:51 PM
 */

package edu.cmu.cs.fluid.mvc.version;

/**
 * Constants for supported Fixed Version attribute inheritance.
 *
 * @author Aaron Greenhosue
 */
public interface FixedVersionSupport
{
  /**
   * Property name for the Version property. The value of this
   * property is a {@link edu.cmu.cs.fluid.version.Version}, and controls
   * the version used to retrieve attribute values.  It is
   * the responsibility of the model to make sure this property
   * is properly maintained.
   */
  public static final String VERSION = "FixedVersionAttributeManager.Version";
}
