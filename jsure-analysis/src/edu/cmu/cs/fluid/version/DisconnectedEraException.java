/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/DisconnectedEraException.java,v 1.3 2003/07/02 20:19:24 thallora Exp $ */
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.FluidRuntimeException;

/** This exception is throw if we attempt to start an era
 * in the "middle of nowehere" in the version tree.
 */
public class DisconnectedEraException extends FluidRuntimeException {
  public DisconnectedEraException() { super(); }
  public DisconnectedEraException(String s) { super(s); }
}
